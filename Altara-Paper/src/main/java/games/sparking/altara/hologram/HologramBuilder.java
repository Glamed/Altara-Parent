package games.sparking.altara.hologram;

import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.hologram.statics.StaticHologramBuilder;
import games.sparking.altara.hologram.updating.UpdatingHologramBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

@Getter
@NoArgsConstructor
public class HologramBuilder {

    private Location location;
    private double lineSpacing = 0.23;
    private HologramClickHandler clickHandler;

    public HologramBuilder at(Location location) {
        this.location = location;
        return this;
    }

    public HologramBuilder withSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    public HologramBuilder clickHandler(HologramClickHandler clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    public StaticHologramBuilder staticHologram() {
        return new StaticHologramBuilder(this);
    }

    public UpdatingHologramBuilder updating() {
        return new UpdatingHologramBuilder(this);
    }

    public Hologram build() {
        throw new UnsupportedOperationException(
                "Call #staticHologram() or #updating() to choose the hologram type.");
    }

}
