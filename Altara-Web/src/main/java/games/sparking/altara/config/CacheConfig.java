package games.sparking.altara.config;

/**
 * Cache names used by the {@code @Cacheable} / {@code @CacheInvalidate} annotations.  Sizes and
 * expiry for each are configured in {@code application.properties} under {@code micronaut.caches}.
 */
public final class CacheConfig {

    public static final String PROFILES       = "profiles";
    public static final String PROFILE_ALTS   = "profileAlts";
    public static final String PROFILE_GRANTS = "profileGrants";
    public static final String RANKS          = "ranks";
    public static final String UUID_BY_NAME   = "uuidByName";
    public static final String UUID_BY_UUID   = "uuidByUuid";

    public static final String PUNISHMENTS               = "punishments";
    public static final String PLAYER_PUNISHMENTS        = "playerPunishments";
    public static final String PLAYER_ACTIVE_PUNISHMENTS = "playerActivePunishments";
    public static final String PLAYER_BAN_STATUS         = "playerBanStatus";

    private CacheConfig() {}
}
