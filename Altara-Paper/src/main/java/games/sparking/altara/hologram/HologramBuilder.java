package games.sparking.altara.hologram;

import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import lombok.Getter;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.bukkit.entity.Player;

/**
 * Fluent builder for {@link Hologram}.
 *
 * <h2>Quick examples</h2>
 *
 * <pre>{@code
 * // Static hologram – same lines for everyone
 * Hologram sign = new HologramBuilder()
 *         .at(spawnLocation)
 *         .lines("&6&lSparking Games", "&7play.sparking.games")
 *         .build();
 *
 * // Per-player hologram that auto-refreshes every 2 seconds
 * Hologram stats = new HologramBuilder()
 *         .at(statsLocation)
 *         .provider(p -> List.of("&eKills: &f" + getKills(p), "&eDeaths: &f" + getDeaths(p)))
 *         .updateInterval(2, TimeUnit.SECONDS)
 *         .clickHandler((p, h, type) -> p.sendMessage("&aYou clicked the stats board!"))
 *         .build();
 *
 * // Only a specific player can see it
 * Hologram personal = new HologramBuilder()
 *         .at(location)
 *         .provider(p -> List.of("&bYour ping: &f" + p.getPing() + " ms"))
 *         .visibleTo(targetPlayer)
 *         .updateIntervalTicks(20)
 *         .build();
 * }</pre>
 */
@Getter
public class HologramBuilder {

    private Location location;
    private double lineSpacing = 0.25;
    private HologramProvider provider;
    private HologramClickHandler clickHandler;
    private long updateIntervalTicks = 0; // 0 = no task
    private Predicate<Player> visibilityFilter;

    // =========================================================================
    // Location
    // =========================================================================

    public HologramBuilder at(Location location) {
        this.location = location;
        return this;
    }

    public HologramBuilder withSpacing(double spacing) {
        this.lineSpacing = spacing;
        return this;
    }

    // =========================================================================
    // Content – static convenience wrappers
    // =========================================================================

    /** Sets fixed lines identical for every player. */
    public HologramBuilder lines(String... lines) {
        this.provider = HologramProvider.fixed(lines);
        return this;
    }

    /** Sets fixed lines identical for every player. */
    public HologramBuilder lines(List<String> lines) {
        this.provider = HologramProvider.fixed(lines);
        return this;
    }

    /** Sets a per-player provider; the lambda is called on each update for each viewer. */
    public HologramBuilder provider(HologramProvider provider) {
        this.provider = provider;
        return this;
    }

    // =========================================================================
    // Click
    // =========================================================================

    public HologramBuilder clickHandler(HologramClickHandler handler) {
        this.clickHandler = handler;
        return this;
    }

    // =========================================================================
    // Auto-update interval
    // =========================================================================

    /**
     * Makes this hologram auto-refresh on a repeating timer.
     * The first refresh fires immediately when {@link Hologram#startUpdateTask()} is called.
     */
    public HologramBuilder updateInterval(long time, TimeUnit unit) {
        this.updateIntervalTicks = unit.toSeconds(time) * 20L;
        return this;
    }

    public HologramBuilder updateIntervalTicks(long ticks) {
        this.updateIntervalTicks = ticks;
        return this;
    }

    // =========================================================================
    // Visibility filter
    // =========================================================================

    /** Restricts the hologram so it is only visible to players matching {@code filter}. */
    public HologramBuilder visibleTo(Predicate<Player> filter) {
        this.visibilityFilter = filter;
        return this;
    }

    /** Shorthand to restrict the hologram to a single player. */
    public HologramBuilder visibleTo(Player player) {
        this.visibilityFilter = p -> p.getUniqueId().equals(player.getUniqueId());
        return this;
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Constructs the {@link Hologram}, registers it with {@link HologramService}, and
     * starts the update task if an interval was configured.
     *
     * <p>The hologram is <b>not</b> automatically spawned – call {@link Hologram#spawn()} or
     * {@link Hologram#spawn(Player)} yourself.
     */
    public Hologram build() {
        Hologram hologram = new Hologram(this);
        hologram.startUpdateTask();
        return hologram;
    }

    /**
     * Shorthand: builds <em>and</em> immediately spawns for all online players.
     */
    public Hologram buildAndSpawn() {
        Hologram hologram = build();
        hologram.spawn();
        return hologram;
    }
}
