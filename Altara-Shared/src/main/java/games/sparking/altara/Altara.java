package games.sparking.altara;

import games.sparking.altara.mongo.MongoService;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.task.impl.AsynchronousTaskChain;
import lombok.Getter;
import lombok.Setter;

public abstract class Altara {

    public static final AsynchronousTaskChain TASK_CHAIN = new AsynchronousTaskChain(true);

    @Getter private static Altara sharedInstance;

    @Getter private static SystemType systemType;

    @Getter private static MongoService mongoService;
    @Getter private static RedisService redis;

    /** The name/identity of this server node (e.g. "Lobby", "Games", "proxy"). */
    @Getter @Setter
    private static String serverIdentifier;

    public Altara(SystemType systemType)  {
        if (Altara.sharedInstance != null) throw new IllegalStateException("Already Initialized");
        Altara.sharedInstance = this;

        Altara.systemType = systemType;
    }

    public abstract void init();
    public abstract void registerCommands();
    public abstract void registerListener();
}
