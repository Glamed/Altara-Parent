package games.sparking.altara.configuration.defaults;

import games.sparking.altara.configuration.StaticConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class MainConfig implements StaticConfiguration {

    private String backendHost = "http://104.243.41.37:25001/";
    private String backendKey = "1234567890";
    private ServerConfig serverConfig;
    private MongoConfig mongoConfig = new MongoConfig();
    private RedisConfig redisConfig = new RedisConfig();
}