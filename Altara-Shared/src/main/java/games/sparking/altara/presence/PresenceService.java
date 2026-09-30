package games.sparking.altara.presence;

import games.sparking.altara.Altara;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PresenceService {

    private static final String KEY_PREFIX = "altara:presence:";
    private static final long TTL_SECONDS = 60L;

    private static final Map<UUID, UUID> localSessions = new ConcurrentHashMap<>();

    private PresenceService() {
    }

    /**
     * Claims ownership of a player's network presence for this Paper server.
     */
    public static UUID claim(UUID playerUuid, String server) {
        UUID sessionId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        String key = key(playerUuid);

        Altara.getRedisService().executeCommand(redis -> {
            redis.hset(key, "server", server);
            redis.hset(key, "sessionId", sessionId.toString());
            redis.hset(key, "connectedAt", String.valueOf(now));
            redis.hset(key, "lastHeartbeat", String.valueOf(now));
            redis.expire(key, TTL_SECONDS);
            return null;
        });

        localSessions.put(playerUuid, sessionId);
        return sessionId;
    }

    /**
     * Returns the player's current authoritative network presence.
     */
    public static PlayerPresence get(UUID playerUuid) {
        Map<String, String> values = Altara.getRedisService().executeCommand(
                redis -> redis.hgetAll(key(playerUuid))
        );

        if (values == null || values.isEmpty()) return null;

        String server = values.get("server");
        String sessionId = values.get("sessionId");
        String connectedAt = values.get("connectedAt");
        String lastHeartbeat = values.get("lastHeartbeat");

        if (server == null || sessionId == null) return null;

        try {
            return new PlayerPresence(
                    playerUuid,
                    server,
                    UUID.fromString(sessionId),
                    parseLong(connectedAt),
                    parseLong(lastHeartbeat)
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Returns true if Redis currently has a presence record for this player.
     */
    public static boolean isOnline(UUID playerUuid) {
        Boolean exists = Altara.getRedisService().executeCommand(
                redis -> redis.exists(key(playerUuid))
        );

        return Boolean.TRUE.equals(exists);
    }

    /**
     * Returns the server currently owning this player.
     */
    public static String getServer(UUID playerUuid) {
        PlayerPresence presence = get(playerUuid);
        return presence == null ? null : presence.getServer();
    }

    /**
     * Refreshes this server's presence record.
     *
     * The heartbeat is only refreshed if this server still owns the same
     * player session.
     */
    public static boolean heartbeat(UUID playerUuid, String server) {
        UUID sessionId = localSessions.get(playerUuid);
        if (sessionId == null) return false;

        String key = key(playerUuid);

        String currentSession = Altara.getRedisService().executeCommand(
                redis -> redis.hget(key, "sessionId")
        );

        if (!sessionId.toString().equals(currentSession)) {
            localSessions.remove(playerUuid, sessionId);
            return false;
        }

        long now = System.currentTimeMillis();

        Altara.getRedisService().executeCommand(redis -> {
            redis.hset(key, "server", server);
            redis.hset(key, "lastHeartbeat", String.valueOf(now));
            redis.expire(key, TTL_SECONDS);
            return null;
        });

        return true;
    }

    /**
     * Releases this server's local session.
     *
     * The Redis record is only deleted if this exact session still owns it.
     * This prevents delayed cleanup on an old server from deleting a newer
     * server's presence record.
     */
    public static boolean release(UUID playerUuid) {
        UUID sessionId = localSessions.remove(playerUuid);
        if (sessionId == null) return false;

        String key = key(playerUuid);

        Object result = Altara.getRedisService().executeCommand(redis -> redis.eval(
                """
                local current = redis.call('HGET', KEYS[1], 'sessionId')
                if current == ARGV[1] then
                    redis.call('DEL', KEYS[1])
                    return 1
                end
                return 0
                """,
                1,
                key,
                sessionId.toString()
        ));

        return result instanceof Long && (Long) result == 1L;
    }

    /**
     * Returns true if another server/session currently owns the player.
     */
    public static boolean hasMoved(UUID playerUuid) {
        UUID localSession = localSessions.get(playerUuid);
        if (localSession == null) return false;

        PlayerPresence presence = get(playerUuid);
        if (presence == null) return false;

        return !localSession.equals(presence.getSessionId());
    }

    public static UUID getLocalSession(UUID playerUuid) {
        return localSessions.get(playerUuid);
    }

    public static void forgetLocalSession(UUID playerUuid) {
        localSessions.remove(playerUuid);
    }

    private static String key(UUID playerUuid) {
        return KEY_PREFIX + playerUuid;
    }

    private static long parseLong(String value) {
        if (value == null) return -1L;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }
}