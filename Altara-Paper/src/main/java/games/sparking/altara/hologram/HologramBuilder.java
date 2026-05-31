package games.sparking.altara.hologram;

import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.hologram.statics.StaticHologramBuilder;
import games.sparking.altara.hologram.updating.UpdatingHologramBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class HologramBuilder {

    private Location location;
    private double lineSpacing = 0.30;
    private HologramClickHandler clickHandler;
    /** Empty = visible to everyone. Non-empty = only these players receive the spawn packet. */
    private final Set<UUID> viewers = new LinkedHashSet<>();

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

    /** Restrict this hologram so only the given player(s) receive the spawn packet. */
    public HologramBuilder visibleTo(Player... players) {
        for (Player p : players) viewers.add(p.getUniqueId());
        return this;
    }

    /** Internal: copy viewers by UUID (used by sub-builder copy constructors). */
    protected HologramBuilder visibleTo(Collection<UUID> uuids) {
        viewers.addAll(uuids);
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
