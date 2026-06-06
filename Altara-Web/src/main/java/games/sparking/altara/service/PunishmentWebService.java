package games.sparking.altara.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.packet.PunishmentIssuedPacket;
import games.sparking.altara.punishment.packet.PunishmentRevokedPacket;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.repository.PunishmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring service for the punishment REST layer.
 *
 * <h3>Caching strategy</h3>
 * <ul>
 *   <li>{@code punishments}        — {@code GET /api/punishment/{id}}</li>
 *   <li>{@code playerPunishments}  — {@code GET /api/punishment/player/{uuid}}</li>
 *   <li>{@code playerActivePunishments} — {@code GET /api/punishment/player/{uuid}/active}</li>
 *   <li>{@code playerBanStatus}    — {@code GET /api/punishment/player/{uuid}/banned}</li>
 * </ul>
 * All mutation operations evict the relevant keys and publish a Redis packet
 * to keep every Paper server in sync.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PunishmentWebService {

    private final PunishmentRepository punishmentRepository;
    private final RedisService redisService;
    private final CacheManager cacheManager;

    // ── Issue ──────────────────────────────────────────────────────────────────

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PLAYER_PUNISHMENTS,        key = "#punishment.get('playerUuid').getAsString()"),
            @CacheEvict(value = CacheConfig.PLAYER_ACTIVE_PUNISHMENTS, key = "#punishment.get('playerUuid').getAsString()"),
            @CacheEvict(value = CacheConfig.PLAYER_BAN_STATUS,         key = "#punishment.get('playerUuid').getAsString()")
    })
    public Optional<JsonObject> issuePunishment(JsonObject punishment) {
        try {
            // Ensure server-side fields are set regardless of what the caller sent.
            if (!punishment.has("id") || punishment.get("id").isJsonNull()) {
                punishment.addProperty("id", UUID.randomUUID().toString());
            }
            if (!punishment.has("issuedAt") || punishment.get("issuedAt").isJsonNull()) {
                punishment.addProperty("issuedAt", System.currentTimeMillis());
            }
            if (!punishment.has("removed")) {
                punishment.addProperty("removed",   false);
                punishment.addProperty("removedAt", -1L);
            }

            JsonObject saved = punishmentRepository.insert(punishment);
            Punishment p = Punishment.fromJson(saved);
            publishIssuedPacket(p);
            return Optional.of(saved);
        } catch (Exception e) {
            log.error("Failed to issue punishment for player {}: {}",
                    punishment.has("playerUuid") ? punishment.get("playerUuid") : "unknown", e.getMessage(), e);
            return Optional.empty();
        }
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    @Cacheable(value = CacheConfig.PUNISHMENTS, key = "#id")
    public Optional<JsonObject> getPunishment(String id) {
        return punishmentRepository.findById(id);
    }

    @Cacheable(value = CacheConfig.PLAYER_PUNISHMENTS, key = "#playerUuid.toString()")
    public JsonArray getPlayerPunishments(UUID playerUuid) {
        return punishmentRepository.findByPlayer(playerUuid.toString());
    }

    @Cacheable(value = CacheConfig.PLAYER_ACTIVE_PUNISHMENTS, key = "#playerUuid.toString()")
    public JsonArray getActivePlayerPunishments(UUID playerUuid) {
        return punishmentRepository.findActiveByPlayer(playerUuid.toString());
    }

    @Cacheable(value = CacheConfig.PLAYER_BAN_STATUS, key = "#playerUuid.toString()")
    public boolean isPlayerBanned(UUID playerUuid) {
        return punishmentRepository.isBanned(playerUuid.toString());
    }

    // ── Revoke ─────────────────────────────────────────────────────────────────

    @CacheEvict(value = CacheConfig.PUNISHMENTS, key = "#id")
    public Optional<JsonObject> revokePunishment(String id, String removedBy) {
        Optional<JsonObject> result = punishmentRepository.revoke(id, removedBy);
        result.ifPresent(p -> {
            String playerUuid = p.has("playerUuid") ? p.get("playerUuid").getAsString() : null;
            if (playerUuid != null) {
                evictPlayerCaches(playerUuid);
                publishRevokedPacket(id, playerUuid, removedBy);
            }
        });
        return result;
    }

    // ── Update (PATCH) ─────────────────────────────────────────────────────────

    /**
     * Applies a partial update to an existing punishment document.
     *
     * <p>Only {@code infractionType}, {@code message}, {@code notes}, and {@code actions}
     * are accepted; all other fields are ignored for safety.
     *
     * @param id      the punishment ID
     * @param updates partial JSON body from the caller
     */
    @CacheEvict(value = CacheConfig.PUNISHMENTS, key = "#id")
    public Optional<JsonObject> updatePunishment(String id, JsonObject updates) {
        Optional<JsonObject> result = punishmentRepository.patch(id, updates);
        result.ifPresent(p -> {
            String playerUuid = p.has("playerUuid") ? p.get("playerUuid").getAsString() : null;
            if (playerUuid != null) {
                evictPlayerCaches(playerUuid);
                try {
                    publishIssuedPacket(Punishment.fromJson(p));
                } catch (Exception e) {
                    log.warn("Could not re-publish updated punishment {}: {}", id, e.getMessage());
                }
            }
        });
        return result;
    }

    private void evictPlayerCaches(String playerUuid) {
        for (String cacheName : List.of(
                CacheConfig.PLAYER_PUNISHMENTS,
                CacheConfig.PLAYER_ACTIVE_PUNISHMENTS,
                CacheConfig.PLAYER_BAN_STATUS)) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) cache.evict(playerUuid);
        }
    }

    // ── Redis propagation ──────────────────────────────────────────────────────

    private void publishIssuedPacket(Punishment punishment) {
        try {
            redisService.publish(new PunishmentIssuedPacket(punishment));
        } catch (Exception e) {
            log.warn("Could not publish PunishmentIssuedPacket for {}: {}", punishment.getId(), e.getMessage());
        }
    }

    private void publishRevokedPacket(String punishmentId, String playerUuid, String revokedBy) {
        try {
            redisService.publish(new PunishmentRevokedPacket(punishmentId, playerUuid, revokedBy));
        } catch (Exception e) {
            log.warn("Could not publish PunishmentRevokedPacket for {}: {}", punishmentId, e.getMessage());
        }
    }
}

