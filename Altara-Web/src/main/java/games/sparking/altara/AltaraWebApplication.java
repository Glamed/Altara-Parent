package games.sparking.altara;

import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.redis.RedisService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.io.File;

@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@EnableCaching
public class AltaraWebApplication {

    public static void main(String[] args) {
        // Load config exactly like every other Altara module (Lobby, Proxy, etc.)
        JsonConfigurationService configService = new JsonConfigurationService();
        MainConfig mainConfig = configService.loadConfiguration(MainConfig.class, new File("config.json"));

        // Init Altara singleton — this connects Mongo + Redis from MainConfig
        new AltaraWeb(mainConfig);

        // Start Spring — server.port comes from application.yml (default 25001)
        SpringApplication app = new SpringApplication(AltaraWebApplication.class);
        app.run(args);
    }

    // ------------------------------------------------------------------
    // Expose Altara-owned singletons as Spring beans so repositories and
    // services can be injected normally by Spring.
    // No separate @Configuration class needed.
    // ------------------------------------------------------------------

    /** MongoTemplate backed by the MongoClient Shared already connected. */
    @Bean
    public MongoTemplate mongoTemplate() {
        var mongo = Altara.getMongoService();
        return new MongoTemplate(
                new SimpleMongoClientDatabaseFactory(mongo.getClient(), mongo.getDatabase().getName())
        );
    }

    /** The RedisService Shared already initialised — injected into web services. */
    @Bean
    public RedisService redisService() {
        return Altara.getRedisService();
    }
}
