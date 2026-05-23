package games.sparking.altara.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures per-cache Caffeine specs used throughout the Altara-Web tier.
 *
 * <h3>Cache names</h3>
 * <ul>
 *   <li>{@code profiles}      – {@link games.sparking.altara.service.ProfileWebService#getProfile}</li>
 *   <li>{@code profileAlts}   – {@link games.sparking.altara.service.ProfileWebService#getAlts}</li>
 *   <li>{@code profileGrants} – {@link games.sparking.altara.service.ProfileWebService#getGrants}</li>
 *   <li>{@code ranks}         – {@link games.sparking.altara.service.RankWebService#getAllRanks} (key {@code "all"})
 *                               and {@link games.sparking.altara.service.RankWebService#getRank} (key = UUID)</li>
 *   <li>{@code uuidByName}    – {@link games.sparking.altara.service.UUIDWebService#resolveNameToJson}</li>
 *   <li>{@code uuidByUuid}    – {@link games.sparking.altara.service.UUIDWebService#resolveUuidToJson}</li>
 * </ul>
 */
@Configuration
public class CacheConfig {

    // ------------------------------------------------------------------
    // Cache names — referenced as constants to avoid magic strings
    // ------------------------------------------------------------------
    public static final String PROFILES       = "profiles";
    public static final String PROFILE_ALTS   = "profileAlts";
    public static final String PROFILE_GRANTS = "profileGrants";
    public static final String RANKS          = "ranks";
    public static final String UUID_BY_NAME   = "uuidByName";
    public static final String UUID_BY_UUID   = "uuidByUuid";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Player profiles — moderate TTL, hot data while player is online
        manager.registerCustomCache(PROFILES,
                Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Alt-account lists — same TTL as profile
        manager.registerCustomCache(PROFILE_ALTS,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Grant arrays embedded in each profile — evicted on every grant mutation
        manager.registerCustomCache(PROFILE_GRANTS,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // Rank objects — changed infrequently; longer TTL is safe
        manager.registerCustomCache(RANKS,
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // UUID ↔ name lookups — high-cardinality; long TTL since names rarely change
        manager.registerCustomCache(UUID_BY_NAME,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        manager.registerCustomCache(UUID_BY_UUID,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        return manager;
    }
}

