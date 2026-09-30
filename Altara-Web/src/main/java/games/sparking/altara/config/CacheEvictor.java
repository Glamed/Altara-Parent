package games.sparking.altara.config;

import io.micronaut.cache.CacheManager;
import jakarta.inject.Singleton;

/**
 * Invalidation for keys that aren't a plain method parameter (e.g. a UUID inside a JSON body),
 * where {@code @CacheInvalidate} can't express the key.  Keys match what {@code @Cacheable}
 * generates for a single-argument method: the argument itself.
 */
@Singleton
public class CacheEvictor {

    private final CacheManager<?> cacheManager;

    public CacheEvictor(CacheManager<?> cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void invalidate(String cacheName, Object key) {
        cacheManager.getCache(cacheName).invalidate(key);
    }

    public void invalidateAll(String cacheName) {
        cacheManager.getCache(cacheName).invalidateAll();
    }

    /** Everything cached about one player's profile, plus name lookups (a name may have changed). */
    public void profile(String uuid) {
        invalidate(CacheConfig.PROFILES, uuid);
        invalidate(CacheConfig.PROFILE_GRANTS, uuid);
        invalidate(CacheConfig.PROFILE_ALTS, uuid);
        invalidate(CacheConfig.UUID_BY_UUID, uuid);
        invalidateAll(CacheConfig.UUID_BY_NAME);
    }

    /** Everything cached about one player's punishments. */
    public void playerPunishments(String playerUuid) {
        invalidate(CacheConfig.PLAYER_PUNISHMENTS, playerUuid);
        invalidate(CacheConfig.PLAYER_ACTIVE_PUNISHMENTS, playerUuid);
        invalidate(CacheConfig.PLAYER_BAN_STATUS, playerUuid);
    }
}
