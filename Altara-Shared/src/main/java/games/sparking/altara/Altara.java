package games.sparking.altara;

import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.configuration.defaults.MongoConfig;
import games.sparking.altara.configuration.defaults.RedisConfig;
import games.sparking.altara.disguise.DisguiseService;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.punishment.PunishmentService;
import games.sparking.altara.task.TaskImplementor;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.mongo.MongoService;
import games.sparking.altara.profile.ProfileService;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.RankService;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.task.impl.AsynchronousTaskChain;
import lombok.Getter;
import lombok.Setter;

import games.sparking.altara.logging.CommonLogger;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class Altara {

    /** Fallback JUL logger used until a platform-specific CommonLogger is set. */
    private static final Logger JUL_FALLBACK = Logger.getLogger("Altara");

    private static final CommonLogger DEFAULT_LOGGER = new CommonLogger() {
        public void info(String msg)  { JUL_FALLBACK.info(msg); }
        public void warn(String msg)  { JUL_FALLBACK.warning(msg); }
        public void error(String msg) { JUL_FALLBACK.severe(msg); }
    };

    private CommonLogger logger = DEFAULT_LOGGER;

    public static final AsynchronousTaskChain TASK_CHAIN = new AsynchronousTaskChain(true);

    @Getter private static Altara sharedInstance;

    @Getter private static SystemType systemType;

    @Getter private final ConfigurationService configurationService;
    @Getter private final MainConfig mainConfig;

    @Getter private static MongoService mongoService;
    @Getter private static RedisService redisService;

    @Getter private final RankService rankService;
    @Getter private final ProfileService profileService;
    @Getter private final PunishmentService punishmentService;
    @Getter private final DisguiseService disguiseService;
    @Getter private final ProfilerService profilerService;

    public Altara(SystemType systemType, ConfigurationService configurationService, MainConfig mainConfig, TaskImplementor taskImplementor)  {
        Altara.systemType = systemType;
        if (Altara.sharedInstance != null) throw new IllegalStateException("Already Initialized");
        Altara.sharedInstance = this;

        Tasks.setTaskImplementor(taskImplementor);

        this.configurationService = configurationService;
        this.mainConfig = mainConfig;

        MongoConfig mongoConfig = mainConfig.getMongoConfig();
        String mongoUri = mongoConfig.isAuthEnabled()
                ? "mongodb://" + mongoConfig.getAuthUsername() + ":" + mongoConfig.getAuthPassword()
                  + "@" + mongoConfig.getHost() + ":" + mongoConfig.getPort() + "/" + mongoConfig.getAuthDatabase()
                : "mongodb://" + mongoConfig.getHost() + ":" + mongoConfig.getPort();
        mongoService = new MongoService(mongoUri, mongoConfig.getAuthDatabase())
                .connect().collectionsInit();

        RedisConfig redisConfig = mainConfig.getRedisConfig();
        String redisPass = (redisConfig.isAuthEnabled() && redisConfig.getAuthPassword() != null
                && !redisConfig.getAuthPassword().isBlank())
                ? redisConfig.getAuthPassword() : null;
        redisService = new RedisService(
                redisConfig.getChannel(),
                redisConfig.getHost(),
                redisConfig.getPort(),
                redisPass
        ).subscribe();

        this.profileService = new ProfileService();
        this.rankService = new RankService();
        this.punishmentService = new PunishmentService();
        this.disguiseService = new DisguiseService();
        this.profilerService = new ProfilerService();

        // The WEB module IS the rank API — skip the HTTP-based rank load that
        // would be attempted before Spring Boot (Tomcat) has started.
        if (systemType != SystemType.WEB) {
            rankService.loadRanks(() -> {});
        }

        startServerMonitor();
    }

    public abstract void init();
    public abstract void registerCommands();
    public abstract void registerListeners();

    public abstract void loadFiles();
    public abstract void saveMainConfig();

    public abstract void startServerMonitor();
    public void handleServerInfoUpdate(ServerInfo serverInfo) {}

    public abstract void dispatchConsoleCommand(String command);

    public abstract void updatePermissions(UUID uuid);
    public abstract void updatePermissionsWithRank(Rank rank);
    public abstract List<String> getLocalPermissions(Rank rank);
    public abstract void saveLocalPermissions(Rank rank);
    public abstract void handleRankDeletion(Rank rank);


    public abstract String getServerNameShort();
    public abstract String getServerNameLong();
    public abstract String getLocalServerName();
    public abstract String getServerGroup();

    public CommonLogger getLogger() {
        return logger;
    }

    public void setLogger(CommonLogger logger) {
        this.logger = logger;
    }
}
