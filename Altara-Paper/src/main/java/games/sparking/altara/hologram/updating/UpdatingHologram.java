package games.sparking.altara.hologram.updating;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramLine;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class UpdatingHologram extends Hologram {

    @Getter private final long interval;
    @Getter private final Supplier<List<String>> linesSupplier;
    @Getter private final HologramProvider provider;
    private BukkitRunnable updateTask;

    /**
     * When a {@link HologramProvider} is in use, this field is set to the
     * player being updated before {@link #spawnFor(Player)} is called so that
     * {@link #getLines()} can return that player's lines.
     *
     * <p>Update runs on the main thread (BukkitRunnable), so no concurrency
     * concerns arise from using a plain instance field.
     */
    private transient Player providerContext;

    protected UpdatingHologram(UpdatingHologramBuilder builder) {
        super(builder);
        if (builder.getInterval() <= 0)
            throw new IllegalArgumentException(
                    "Please set an update interval using UpdatingHologramBuilder#interval");
        if (builder.getProvider() == null && builder.getLinesSupplier() == null)
            throw new IllegalArgumentException(
                    "Please set a provider (UpdatingHologramBuilder#provider) " +
                    "or a lines supplier (UpdatingHologramBuilder#lines)");
        this.interval      = builder.getInterval();
        this.linesSupplier = builder.getLinesSupplier();
        this.provider      = builder.getProvider();
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

    /**
     * Overrides the base spawn to set {@link #providerContext} before
     * {@link #getLines()} is called, so the provider can supply the correct
     * per-player lines regardless of which call site triggers the spawn
     * (join listener, {@link #spawn()}, or {@link #update()}).
     */
    @Override
    public void spawnFor(Player player) {
        if (provider != null) providerContext = player;
        try {
            super.spawnFor(player);
        } finally {
            providerContext = null;
        }
    }

    /**
     * Returns the hologram lines for the current spawn/update context.
     *
     * <ul>
     *   <li>If a {@link HologramProvider} is set, delegates to
     *       {@code provider.getLines(providerContext)}.  {@link #providerContext}
     *       is always non-null here because {@link #spawnFor} sets it.</li>
     *   <li>Otherwise falls back to the {@link #linesSupplier}.</li>
     * </ul>
     */
    @Override
    public List<HologramLine> getLines() {
        if (provider != null) {
            Player p = providerContext;
            List<String> raw = (p != null) ? provider.getLines(p) : Collections.emptyList();
            List<HologramLine> lines = new ArrayList<>(raw.size());
            for (String s : raw) lines.add(new HologramLine(s));
            return lines;
        }
        List<String> raw = linesSupplier.get();
        List<HologramLine> lines = new ArrayList<>(raw.size());
        for (String s : raw) lines.add(new HologramLine(s));
        return lines;
    }

    /**
     * Updates the hologram for all current viewers.
     *
     * <ul>
     *   <li>Provider mode: fetches per-player lines from the provider and attempts
     *       an in-place metadata update (no entity destroy/respawn = no flicker).
     *       Falls back to a full {@link #spawnFor} only when the line count changes.</li>
     *   <li>Supplier mode: delegates to the base class metadata-update path.</li>
     * </ul>
     */
    @Override
    public void update() {
        if (provider != null) {
            for (UUID uuid : new ArrayList<>(getPlayerLines().keySet())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;

                // Ask the provider for this player's current lines.
                List<String> raw = provider.getLines(player);
                List<HologramLine> newLines = new ArrayList<>(raw.size());
                for (String s : raw) newLines.add(new HologramLine(s));

                // Fast path: update text metadata only — no destroy/respawn, no flicker.
                // Slow path: line count changed, full respawn needed.
                if (!updateTextInPlace(player, newLines)) {
                    spawnFor(player);   // providerContext set inside spawnFor override
                }
            }
        } else {
            super.update();
        }
    }
}
