package games.sparking.altara.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.task.Tasks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Shared punishment service used by every Altara module (Paper, Proxy, Web).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Issue / revoke punishments via the Altara-Web REST API.</li>
 *   <li>Maintain a JVM-local cache (UUID → punishment list) so hot-path
 *       checks (e.g. "is banned?") don't need a network round-trip every time.</li>
 *   <li>Expose cache-mutation helpers called by incoming Redis packets.</li>
 * </ul>
 */
public class PunishmentService {

    /** JVM-local cache: playerUuid → punishment list (may include expired/removed records) */
    private final Map<UUID, List<Punishment>> cache = new ConcurrentHashMap<>();

    // ── Issue ──────────────────────────────────────────────────────────────────

    /**
     * Issue a punishment by posting to the Web API.
     * The Web layer will persist it to MongoDB and publish a {@code PunishmentIssuedPacket}
     * to Redis so every Paper server applies the effect in real-time.
     */
    public void issuePunishment(UUID staffUuid,
                                UUID playerUuid,
                                InfractionType infractionType,
                                List<RestrictionAction> actions,
                                String message,
                                Consumer<Punishment> callback,
                                boolean async) {
        if (async) {
            Tasks.runAsync(() -> issuePunishment(staffUuid, playerUuid, infractionType, actions, message, callback, false));
            return;
        }

        Punishment punishment = new Punishment(playerUuid, staffUuid, infractionType, actions, message);
        RequestResponse response = RequestHandler.post("api/punishment", punishment.toJson());

        if (response.wasSuccessful()) {
            Punishment saved = Punishment.fromJson(response.asObject());
            cache.computeIfAbsent(playerUuid, k -> Collections.synchronizedList(new ArrayList<>())).add(saved);
            if (callback != null) callback.accept(saved);
        } else {
            if (callback != null) callback.accept(null);
        }
    }

    /** Blocking convenience overload (no callback). */
    public Punishment issuePunishment(UUID staffUuid,
                                      UUID playerUuid,
                                      InfractionType infractionType,
                                      List<RestrictionAction> actions,
                                      String message) {
        Punishment[] result = {null};
        issuePunishment(staffUuid, playerUuid, infractionType, actions, message, p -> result[0] = p, false);
        return result[0];
    }

    // ── Revoke ─────────────────────────────────────────────────────────────────

    /**
     * Revoke (soft-delete) a punishment by ID.
     * The Web API marks it removed and publishes a {@code PunishmentRevokedPacket}.
     */
    public boolean revokePunishment(String punishmentId, UUID revokedBy) {
        String url = revokedBy != null
                ? "api/punishment/%s?removedBy=%s"
                : "api/punishment/%s";
        RequestResponse response = revokedBy != null
                ? RequestHandler.delete(url, punishmentId, revokedBy.toString())
                : RequestHandler.delete(url, punishmentId);

        if (response.wasSuccessful()) {
            cache.values().forEach(list -> list.removeIf(p -> p.getId().equals(punishmentId)));
            return true;
        }
        return false;
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /** Returns all records for a player (loads from API if not cached). */
    public List<Punishment> getPunishments(UUID playerUuid) {
        return cache.containsKey(playerUuid) ? cache.get(playerUuid) : loadPunishments(playerUuid);
    }

    /** Returns only currently-active punishments. */
    public List<Punishment> getActivePunishments(UUID playerUuid) {
        return getPunishments(playerUuid).stream()
                .filter(Punishment::isActive)
                .toList();
    }

    /** Returns {@code true} if the player has an active SUSPENSION (ban). */
    public boolean isPlayerBanned(UUID playerUuid) {
        return getActivePunishments(playerUuid).stream().anyMatch(Punishment::isBan);
    }

    /** Returns the first active ban punishment, or {@code null} if the player is not banned. */
    public Punishment getActiveBan(UUID playerUuid) {
        return getActivePunishments(playerUuid).stream()
                .filter(Punishment::isBan)
                .findFirst()
                .orElse(null);
    }

    /** Returns {@code true} if the player has an active CHAT_RESTRICTION. */
    public boolean isChatMuted(UUID playerUuid) {
        return getActivePunishments(playerUuid).stream()
                .anyMatch(p -> p.getActions().stream()
                        .anyMatch(a -> a.getType() == PunishmentType.CHAT_RESTRICTION
                                && !a.hasExpired(p.getIssuedAt())));
    }

    public Punishment getPunishment(String id) {
        RequestResponse response = RequestHandler.get("api/punishment/%s", id);
        return response.wasSuccessful() ? Punishment.fromJson(response.asObject()) : null;
    }

    // ── Loading ────────────────────────────────────────────────────────────────

    /**
     * Fetches all punishment records for a player from the Web API and caches them locally.
     */
    public List<Punishment> loadPunishments(UUID playerUuid) {
        RequestResponse response = RequestHandler.get("api/punishment/player/%s", playerUuid.toString());
        if (!response.wasSuccessful()) return Collections.emptyList();

        List<Punishment> list = new ArrayList<>();
        JsonArray arr = response.asArray();
        if (arr != null) {
            for (JsonElement el : arr) {
                list.add(Punishment.fromJson(el.getAsJsonObject()));
            }
        }
        cache.put(playerUuid, Collections.synchronizedList(list));
        return list;
    }

    /** Async variant for use on the main server thread. */
    public void loadPunishments(UUID playerUuid, Consumer<List<Punishment>> callback, boolean async) {
        if (async) {
            Tasks.runAsync(() -> loadPunishments(playerUuid, callback, false));
            return;
        }
        callback.accept(loadPunishments(playerUuid));
    }

    // ── Cache helpers (called by Redis packet receivers) ───────────────────────

    /** Upserts one punishment into the local cache (called when a packet arrives). */
    public void updateCacheFromPacket(Punishment punishment) {
        if (punishment.getPlayerUuid() == null) return;
        UUID playerUuid = UUID.fromString(punishment.getPlayerUuid());
        List<Punishment> list = cache.computeIfAbsent(playerUuid,
                k -> Collections.synchronizedList(new ArrayList<>()));
        list.removeIf(p -> p.getId().equals(punishment.getId()));
        list.add(punishment);
    }

    /** Removes one punishment from the local cache (called when a revoke packet arrives). */
    public void removeFromCacheFromPacket(String punishmentId, UUID playerUuid) {
        List<Punishment> list = cache.get(playerUuid);
        if (list != null) list.removeIf(p -> p.getId().equals(punishmentId));
    }

    public void invalidateCache(UUID playerUuid) {
        cache.remove(playerUuid);
    }
}
