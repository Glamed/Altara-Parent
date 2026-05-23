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
public class MongoConfig implements StaticConfiguration {

    private String host = "10.0.0.55";
    private int port = 27017;
    private boolean authEnabled = false;
    private String authUsername = "admin";
    private String authPassword = "thankyou2";
    private String authDatabase = "mcfriends";

}