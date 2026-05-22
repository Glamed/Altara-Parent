package games.sparking.altara.configuration.defaults;

import games.sparking.altara.configuration.StaticConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.bukkit.Location;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class LocationConfig extends SimpleLocationConfig implements StaticConfiguration {

    private float pitch = 0;
    private float yaw = 0;

    public LocationConfig(Location location) {
        this(location, false);
    }
    public LocationConfig(Location location, boolean block) {
        setLocation(location, block);
    }

    @Override
    public void setLocation(Location location, boolean block) {
        super.setLocation(location, block);
        this.pitch = location.getPitch();
        this.yaw = location.getYaw();
    }

    @Override
    public Location getLocation() {
        Location location = super.getLocation();
        location.setPitch(this.pitch);
        location.setYaw(this.yaw);
        return location;
    }

    @Override
    public void setLocation(Location location) {
        this.setLocation(location, false);
    }
}