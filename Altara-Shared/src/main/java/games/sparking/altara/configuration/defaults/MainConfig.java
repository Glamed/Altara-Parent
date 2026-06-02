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

    private String backendHost = "http://10.0.0.55:25001/";
    private String backendKey = "1234567890";

    private MongoConfig mongoConfig = new MongoConfig();
    private RedisConfig redisConfig = new RedisConfig();
    private ServerConfig serverConfig  = new ServerConfig();

    private int staffWeight = 160;
    private int adminWeight = 210;
    private int ownerWeight = 280;
}