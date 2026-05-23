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
public class RedisConfig implements StaticConfiguration {

    private String channel = "altara";
    private String host = "10.0.0.55";
    private int port = 6379;
    private boolean authEnabled = false;
    private String authPassword = "thankyou2";
    private int dbId = 0;

}
