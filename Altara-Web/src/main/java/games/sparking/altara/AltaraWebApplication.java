package games.sparking.altara;

import com.mongodb.client.MongoDatabase;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.redis.RedisService;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;

import java.io.File;

/**
 * Entry point.  Altara is booted first (config.json, MongoDB, Redis) exactly like every other
 * module, then Micronaut starts the HTTP server on top of the connections it already made.
 */
@Factory
public class AltaraWebApplication {

    public static void main(String[] args) {
        MainConfig mainConfig = new JsonConfigurationService().loadConfiguration(MainConfig.class, new File("config.json"));
        new AltaraWeb(mainConfig);

        Micronaut.run(AltaraWebApplication.class, args);
    }

    /** The database Shared already connected to. */
    @Singleton
    MongoDatabase mongoDatabase() {
        return Altara.getMongoService().getDatabase();
    }

    /** The RedisService Shared already initialised. */
    @Singleton
    RedisService redisService() {
        return Altara.getRedisService();
    }
}
