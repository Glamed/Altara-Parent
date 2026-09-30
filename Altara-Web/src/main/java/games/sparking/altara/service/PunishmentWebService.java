package games.sparking.altara.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.config.CacheEvictor;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.packet.PunishmentIssuedPacket;
import games.sparking.altara.punishment.packet.PunishmentRevokedPacket;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.repository.PunishmentRepository;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Punishments.  Issuing, revoking and editing publish a Redis packet so every game server
 * applies the change live; marking one notified deliberately doesn't.
 */
@Singleton
@RequiredArgsConstructor
@Slf4j
public class PunishmentWebService {

    private final PunishmentRepository punishmentRepository;
    private final RedisService redisService;
    private final CacheEvictor cacheEvictor;

    // ── Issue ──────────────────────────────────────────────────────────────────

    public Optional<JsonObject> issuePunishment(JsonObject punishment) {
        // Server-side defaults, whatever the caller sent.
        if (!punishment.has("id") || punishment.get("id").isJsonNull()) {
            punishment.addProperty("id", UUID.randomUUID().toString());
        }
        if (!punishment.has("issuedAt") || punishment.get("issuedAt").isJsonNull()) {
            punishment.addProperty("issuedAt", System.currentTimeMillis());
        }
        if (!punishment.has("notified") || punishment.get("notified").isJsonNull()) {
            punishment.addProperty("notified", false);
        }
        if (!punishment.has("removed")) {
            punishment.addProperty("removed", false);
            punishment.addProperty("removedAt", -1L);
        }

        try {
            JsonObject saved = punishmentRepository.insert(punishment);
            cacheEvictor.playerPunishments(punishment.get("playerUuid").getAsString());
            publish(new PunishmentIssuedPacket(Punishment.fromJson(saved)));
            return Optional.of(saved);
        } catch (Exception e) {
            log.error("Failed to issue punishment for player {}", punishment.get("playerUuid"), e);
            return Optional.empty();
        }
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    @Cacheable(CacheConfig.PUNISHMENTS)
    public Optional<JsonObject> getPunishment(String id) {
        return punishmentRepository.findById(id);
    }

    @Cacheable(CacheConfig.PLAYER_PUNISHMENTS)
    public JsonArray getPlayerPunishments(String playerUuid) {
        return punishmentRepository.findByPlayer(playerUuid);
    }

    @Cacheable(CacheConfig.PLAYER_ACTIVE_PUNISHMENTS)
    public JsonArray getActivePlayerPunishments(String playerUuid) {
        return punishmentRepository.findActiveByPlayer(playerUuid);
    }

    @Cacheable(CacheConfig.PLAYER_BAN_STATUS)
    public boolean isPlayerBanned(String playerUuid) {
        return punishmentRepository.isBanned(playerUuid);
    }

    // ── Revoke ─────────────────────────────────────────────────────────────────

    @CacheInvalidate(cacheNames = CacheConfig.PUNISHMENTS, parameters = "id")
    public Optional<JsonObject> revokePunishment(String id, String removedBy) {
        Optional<JsonObject> result = punishmentRepository.revoke(id, removedBy);
        result.map(PunishmentWebService::playerOf).ifPresent(playerUuid -> {
            cacheEvictor.playerPunishments(playerUuid);
            publish(new PunishmentRevokedPacket(id, playerUuid, removedBy));
        });
        return result;
    }

    // ── Notification ───────────────────────────────────────────────────────────

    /**
     * Records that the player was shown this punishment.  No packet: servers already know,
     * and re-publishing would re-run enforcement (kicks) for nothing.
     */
    @CacheInvalidate(cacheNames = CacheConfig.PUNISHMENTS, parameters = "id")
    public Optional<JsonObject> markNotified(String id) {
        Optional<JsonObject> result = punishmentRepository.markNotified(id);
        result.map(PunishmentWebService::playerOf).ifPresent(cacheEvictor::playerPunishments);
        return result;
    }

    // ── Leaderboards ─────────────────────────────────────────────────────────────

    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    /**
     * Top staff by actions issued in the last 30 days: staffUuid → (action type, or
     * {@code "TOTAL"}) → count.  Deliberately uncached — this is a low-traffic staff-only
     * view and should reflect anything issued moments ago.
     */
    public JsonObject getStaffLeaderboard() {
        return countByKey(punishmentRepository.findIssuedSince(THIRTY_DAYS_MS), "staffUuid");
    }

    /** Top players by punishments received, all time: playerUuid → (action type, or "TOTAL") → count. */
    public JsonObject getPlayerLeaderboard() {
        return countByKey(punishmentRepository.findIssuedSince(0L), "playerUuid");
    }

    /**
     * Buckets raw punishment records by {@code keyField} (staffUuid or playerUuid), counting
     * each action by its type plus a running {@code TOTAL}.  Records missing the key field
     * (e.g. console-issued punishments when grouping by staff) are skipped.
     */
    private static JsonObject countByKey(JsonArray records, String keyField) {
        Map<String, Map<String, Long>> counts = new HashMap<>();

        for (JsonElement element : records) {
            JsonObject record = element.getAsJsonObject();
            if (!record.has(keyField) || record.get(keyField).isJsonNull()) continue;
            if (!record.has("actions") || !record.get("actions").isJsonArray()) continue;

            String key = record.get(keyField).getAsString();
            Map<String, Long> perType = counts.computeIfAbsent(key, k -> new HashMap<>());

            for (JsonElement actionElement : record.get("actions").getAsJsonArray()) {
                JsonObject action = actionElement.getAsJsonObject();
                String type = action.has("type") && !action.get("type").isJsonNull()
                        ? action.get("type").getAsString() : "UNKNOWN";
                perType.merge(type, 1L, Long::sum);
                perType.merge("TOTAL", 1L, Long::sum);
            }
        }

        JsonObject result = new JsonObject();
        for (Map.Entry<String, Map<String, Long>> entry : counts.entrySet()) {
            JsonObject perType = new JsonObject();
            for (Map.Entry<String, Long> typeCount : entry.getValue().entrySet()) {
                perType.addProperty(typeCount.getKey(), typeCount.getValue());
            }
            result.add(entry.getKey(), perType);
        }
        return result;
    }

    // ── Update (PATCH) ─────────────────────────────────────────────────────────

    /** Partial update; see {@link PunishmentRepository#patch} for the accepted fields. */
    @CacheInvalidate(cacheNames = CacheConfig.PUNISHMENTS, parameters = "id")
    public Optional<JsonObject> updatePunishment(String id, JsonObject updates) {
        Optional<JsonObject> result = punishmentRepository.patch(id, updates);
        result.ifPresent(punishment -> {
            String playerUuid = playerOf(punishment);
            if (playerUuid == null) return;
            cacheEvictor.playerPunishments(playerUuid);
            try {
                publish(new PunishmentIssuedPacket(Punishment.fromJson(punishment)));
            } catch (RuntimeException e) {
                log.warn("Could not re-publish updated punishment {}: {}", id, e.getMessage());
            }
        });
        return result;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String playerOf(JsonObject punishment) {
        return punishment.has("playerUuid") && !punishment.get("playerUuid").isJsonNull()
                ? punishment.get("playerUuid").getAsString() : null;
    }

    private void publish(Packet packet) {
        try {
            redisService.publish(packet);
        } catch (Exception e) {
            log.warn("Could not publish {}: {}", packet.getClass().getSimpleName(), e.getMessage());
        }
    }
}
