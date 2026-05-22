package games.sparking.altara.profile;

import games.sparking.altara.configuration.StaticConfiguration;
import games.sparking.altara.configuration.defaults.LocationConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.bukkit.entity.Player;

import java.util.UUID;

@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class OfflineProfile implements StaticConfiguration {

    private UUID uuid;
    private LocationConfig lastLocation;

    public OfflineProfile(Player player) {
        LocationConfig location = new LocationConfig();
        location.setLocation(player.getLocation());
        this.uuid = player.getUniqueId();
        this.lastLocation = location;
    }

}