package games.sparking.altara.game.module.capturepoint;

import games.sparking.altara.game.module.GameModule;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.BlockInventoryHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages all {@link CapturePoint}s for a game session.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CapturePointModule cpModule = addModule(new CapturePointModule());
 *
 * // During onStart(), after loading your arena world:
 * for (Location loc : arena.getData("capture_point_red")) {
 *     cpModule.addCapturePoint("Red", ChatColor.RED, loc);
 * }
 * }</pre>
 *
 * <p>Capture points are created when the module is enabled (i.e. when the game goes
 * {@code Live}). Add all capture-point locations before or during {@code onStart()}.
 *
 * <p><b>Session isolation:</b> wool/beacon blocks are inside the game's arena world, so
 * updates are naturally bounded. Beacon-inventory cancel checks the block world explicitly.
 */
public class CapturePointModule extends GameModule {

    // -------------------------------------------------------------------------
    // Pending and live data
    // -------------------------------------------------------------------------

    private record PendingPoint(String name, ChatColor colour, Location center) {}

    private final List<PendingPoint>  pendingPoints  = new ArrayList<>();
    private final List<CapturePoint>  capturePoints  = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Builder API
    // -------------------------------------------------------------------------

    /**
     * Registers a capture point to be created when the module enables.
     * Call this inside {@link games.sparking.altara.game.impl.Game#onStart()}.
     *
     * @param name   display name of the point (e.g. "Red", "Blue", "Middle")
     * @param colour chat colour used in messages and the progress display
     * @param center the exact centre location of the point
     * @return {@code this} for chaining
     */
    public CapturePointModule addCapturePoint(String name, ChatColor colour, Location center) {
        pendingPoints.add(new PendingPoint(name, colour, center));
        return this;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onEnable() {
        for (PendingPoint p : pendingPoints) {
            capturePoints.add(new CapturePoint(getGame(), p.name(), p.colour(), p.center()));
        }
    }

    @Override
    protected void onDisable() {
        capturePoints.clear();
        pendingPoints.clear();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC) return;
        if (!getGame().isLive()) return;

        for (CapturePoint point : capturePoints) {
            point.update();
        }
    }

    /**
     * Prevent players from changing beacon upgrades while the game is live.
     * Scoped to this game's arena world.
     */
    @EventHandler
    public void onBeaconOpen(InventoryOpenEvent event) {
        if (!getGame().isLive()) return;
        if (!(event.getInventory().getHolder() instanceof BlockInventoryHolder holder)) return;

        // Check this beacon is inside our arena world
        if (!isArenaWorld(holder.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /** An unmodifiable view of all active capture points. */
    public List<CapturePoint> getCapturePoints() {
        return Collections.unmodifiableList(capturePoints);
    }

    /**
     * Returns {@code true} if {@code location} is within the capture radius of any point.
     * Useful for game-rules that require the player to be on a point.
     */
    public boolean isOnAnyPoint(Location location) {
        for (CapturePoint point : capturePoints) {
            if (point.isOnPoint(location)) return true;
        }
        return false;
    }

    /**
     * Builds a compact scoreboard-style string like {@code "§cRed §9Blue §aMiddle"}
     * showing the current owner colour of every point.
     */
    public String buildDisplayString() {
        StringBuilder sb = new StringBuilder();
        for (CapturePoint point : capturePoints) {
            sb.append(point.getDisplayString()).append(' ');
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isArenaWorld(org.bukkit.World world) {
        games.sparking.altara.world.AltaraWorld arena = getGame().getArenaWorld();
        return arena != null && arena.getWorld().equals(world);
    }
}

