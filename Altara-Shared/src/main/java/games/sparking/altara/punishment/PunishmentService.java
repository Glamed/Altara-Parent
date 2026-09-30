package games.sparking.altara.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    /** Punishments this server has already shown to their player (guards against double notices). */
    private final Set<String> shownHere = ConcurrentHashMap.newKeySet();

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
            markRevoked(punishmentId, revokedBy != null ? revokedBy.toString() : "Console");
            return true;
        }
        return false;
    }

    /** Keeps the record in history but marks it inactive. */
    private void markRevoked(String punishmentId, String revokedBy) {
        long now = System.currentTimeMillis();
        for (List<Punishment> list : cache.values()) {
            synchronized (list) {
                for (Punishment punishment : list) {
                    if (punishment.getId().equals(punishmentId)) {
                        punishment.setRemoved(true);
                        punishment.setRemovedAt(now);
                        punishment.setRemovedBy(revokedBy);
                    }
                }
            }
        }
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /** Returns all records for a player (loads from API if not cached). */
    public List<Punishment> getPunishments(UUID playerUuid) {
        return cache.containsKey(playerUuid) ? cache.get(playerUuid) : loadPunishments(playerUuid);
    }

    /** The cached history, or {@code null} if it hasn't been loaded — never blocks. */
    public List<Punishment> getCachedPunishments(UUID playerUuid) {
        return cache.get(playerUuid);
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
                try {
                    list.add(Punishment.fromJson(el.getAsJsonObject()));
                } catch (RuntimeException e) {
                    // One malformed record shouldn't hide the rest of the history.
                    e.printStackTrace();
                }
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

    // ── Player notification ────────────────────────────────────────────────────

    /**
     * Claims the right to show {@code punishment} to its player.  Returns {@code true} at most
     * once per punishment on this server, and never for one the player has already seen or
     * that was revoked.  After showing it, call {@link #markNotified}.
     */
    public boolean claimNotification(Punishment punishment) {
        if (punishment == null || punishment.getId() == null) return false;
        if (punishment.isNotified() || punishment.isRemoved()) return false;
        return shownHere.add(punishment.getId());
    }

    /** Records that the player has seen the punishment, locally and in the API (async). */
    public void markNotified(Punishment punishment) {
        punishment.setNotified(true);
        List<Punishment> list = punishment.getPlayerUuid() == null ? null : cache.get(UUID.fromString(punishment.getPlayerUuid()));
        if (list != null) {
            synchronized (list) {
                list.stream().filter(p -> p.getId().equals(punishment.getId())).forEach(p -> p.setNotified(true));
            }
        }
        Tasks.runAsync(() -> RequestHandler.post("api/punishment/%s/notified", new JsonObject(), punishment.getId()));
    }

    /** Cached punishments the player hasn't been shown yet, oldest first. */
    public List<Punishment> getUnnotified(UUID playerUuid) {
        List<Punishment> list = cache.get(playerUuid);
        if (list == null) return List.of();
        synchronized (list) {
            return list.stream()
                    .filter(p -> !p.isNotified() && !p.isRemoved())
                    .sorted(Comparator.comparingLong(Punishment::getIssuedAt))
                    .toList();
        }
    }

    // ── Leaderboards ───────────────────────────────────────────────────────────

    /**
     * Raw action counts per staff member for the last 30 days: staffUuid → (action type
     * name, or {@code "TOTAL"} for the sum across all types) → count.  This is a network
     * call every time — ranking, display names and caching are left to the caller, since
     * this is only ever used to build a leaderboard on demand (not a hot path).
     */
    public Map<UUID, Map<String, Long>> getStaffActionCounts() {
        return fetchActionCounts("api/punishment/leaderboard/staff");
    }

    /** Raw action counts per player, all time: playerUuid → (action type, or "TOTAL") → count. */
    public Map<UUID, Map<String, Long>> getPlayerActionCounts() {
        return fetchActionCounts("api/punishment/leaderboard/players");
    }

    private Map<UUID, Map<String, Long>> fetchActionCounts(String endpoint) {
        Map<UUID, Map<String, Long>> result = new HashMap<>();
        RequestResponse response = RequestHandler.get(endpoint);
        if (!response.wasSuccessful()) return result;

        JsonObject root = response.asObject();
        if (root == null) return result;

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException e) {
                continue; // malformed key — skip rather than fail the whole leaderboard
            }
            if (!entry.getValue().isJsonObject()) continue;

            Map<String, Long> counts = new HashMap<>();
            for (Map.Entry<String, JsonElement> typeEntry : entry.getValue().getAsJsonObject().entrySet()) {
                counts.put(typeEntry.getKey(), typeEntry.getValue().getAsLong());
            }
            result.put(uuid, counts);
        }
        return result;
    }

    // ── Cache helpers (called by Redis packet receivers) ───────────────────────

    /** Upserts one punishment into the local cache (called when a packet arrives). */
    public void updateCacheFromPacket(Punishment punishment) {
        if (punishment.getPlayerUuid() == null) return;
        UUID playerUuid = UUID.fromString(punishment.getPlayerUuid());
        List<Punishment> list = cache.computeIfAbsent(playerUuid,
                k -> Collections.synchronizedList(new ArrayList<>()));
        // A re-published record (e.g. after an edit) mustn't undo a notice already shown here.
        if (shownHere.contains(punishment.getId())) punishment.setNotified(true);
        list.removeIf(p -> p.getId().equals(punishment.getId()));
        list.add(punishment);
    }

    /** Marks one punishment revoked in the local cache (called when a revoke packet arrives). */
    public void removeFromCacheFromPacket(String punishmentId, UUID playerUuid, String revokedBy) {
        if (cache.containsKey(playerUuid)) markRevoked(punishmentId, revokedBy);
    }

    public void invalidateCache(UUID playerUuid) {
        cache.remove(playerUuid);
    }
}
