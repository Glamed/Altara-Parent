package games.sparking.altara.hologram.updating;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramLine;
import lombok.Getter;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UpdatingHologram extends Hologram {

    @Getter private final long interval;
    @Getter private final Supplier<List<String>> linesSupplier;
    private BukkitRunnable updateTask;

    protected UpdatingHologram(UpdatingHologramBuilder builder) {
        super(builder);
        if (builder.getInterval() <= 0)
            throw new IllegalArgumentException(
                    "Please set an update interval using UpdatingHologramBuilder#interval");
        if (builder.getLinesSupplier() == null)
            throw new IllegalArgumentException(
                    "Please set a lines supplier using UpdatingHologramBuilder#lines");
        this.interval       = builder.getInterval();
        this.linesSupplier  = builder.getLinesSupplier();
    }

    public void start() {
        if (updateTask != null) return;
        updateTask = new BukkitRunnable() {
            @Override public void run() { update(); }
        };
        updateTask.runTaskTimer(AltaraPaper.getPlugin(), 0L, interval);
    }

    public void cancel() {
        if (updateTask == null) return;
        updateTask.cancel();
        updateTask = null;
    }

    @Override
    public List<HologramLine> getLines() {
        List<HologramLine> lines = new ArrayList<>();
        for (String line : linesSupplier.get())
            lines.add(new HologramLine(line));
        return lines;
    }
}
