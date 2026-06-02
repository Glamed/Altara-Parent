package games.sparking.altara.hologram.updating;

import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Getter(AccessLevel.PROTECTED)
@NoArgsConstructor
public class UpdatingHologramBuilder extends HologramBuilder {

    private long interval = -1;
    private Supplier<List<String>> linesSupplier;
    private HologramProvider provider;

    public UpdatingHologramBuilder(HologramBuilder builder) {
        this.at(builder.getLocation());
        this.withSpacing(builder.getLineSpacing());
        this.clickHandler(builder.getClickHandler());
        if (!builder.getViewers().isEmpty())
            this.visibleTo(builder.getViewers());
    }

    public UpdatingHologramBuilder interval(long time, TimeUnit unit) {
        this.interval = unit.toSeconds(time) * 20;
        return this;
    }

    public UpdatingHologramBuilder intervalTicks(long ticks) {
        this.interval = ticks;
        return this;
    }

    /** Supply a lambda that returns the current lines on each update tick. */
    public UpdatingHologramBuilder lines(Supplier<List<String>> supplier) {
        this.linesSupplier = supplier;
        return this;
    }

    /**
     * Supply a {@link HologramProvider} that returns per-player lines on each
     * update tick.  Takes priority over {@link #lines(Supplier)} when both are set.
     */
    public UpdatingHologramBuilder provider(HologramProvider provider) {
        this.provider = provider;
        return this;
    }

    @Override
    public UpdatingHologramBuilder clickHandler(HologramClickHandler clickHandler) {
        super.clickHandler(clickHandler);
        return this;
    }

    @Override
    public UpdatingHologram build() {
        return new UpdatingHologram(this);
    }
}
