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
public class ServerConfig implements StaticConfiguration {

    private String serverNameLong = "Sparking Games";
    private String serverNameShort = "Sparking";
    private String serverType = "Lobby";
    private String localServerName = "Lobby-1";
    private String website = "sparking.games";
    private String ip = "play.sparking.games";
    private String store = "store.sparking.games";

}