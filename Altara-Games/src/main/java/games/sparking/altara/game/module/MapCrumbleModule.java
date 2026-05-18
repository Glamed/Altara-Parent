package games.sparking.altara.game.module;

import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import games.sparking.altara.world.AltaraWorld;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;

import java.util.Random;

/**
 * Gradually removes random surface blocks from the arena after a configurable delay.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // inside onStart():
 * addModule(new MapCrumbleModule(100_000L)   // start after 100 s
 *         .blocksPerTick(3)
 *         .startMessage("§cThe world crumbles!"));
 * }</pre>
 */
public class MapCrumbleModule extends GameModule {

    private final long delayMs;
    private int         blocksPerTick = 3;
    private String      startMessage  = ChatColor.RED + "The world is crumbling!";

    private boolean started = false;
    private final Random rng = new Random();

    /** @param delayMs milliseconds after game start before crumbling begins */
    public MapCrumbleModule(long delayMs) {
        this.delayMs = delayMs;
    }

    public MapCrumbleModule blocksPerTick(int n)       { this.blocksPerTick = n;   return this; }
    public MapCrumbleModule startMessage(String msg)   { this.startMessage  = msg; return this; }

    @Override
    protected void onEnable() {
        started = false;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK) return;
        if (!getGame().isLive()) return;

        AltaraWorld arena = getGame().getArenaWorld();
        if (arena == null) return;

        if (System.currentTimeMillis() - getGame().getStartTime() < delayMs) return;

        if (!started) {
            started = true;
            getGame().broadcast(startMessage);
        }

        crumble(arena);
    }

    private void crumble(AltaraWorld arena) {
        World world = arena.getWorld();
        Location min = arena.getMin();
        Location max = arena.getMax();
        int width = Math.max(1, max.getBlockX() - min.getBlockX());
        int depth = Math.max(1, max.getBlockZ() - min.getBlockZ());

        for (int i = 0; i < blocksPerTick; i++) {
            int x = min.getBlockX() + rng.nextInt(width);
            int z = min.getBlockZ() + rng.nextInt(depth);
            for (int y = max.getBlockY(); y >= min.getBlockY(); y--) {
                Block block = world.getBlockAt(x, y, z);
                if (!block.getType().isAir()) {
                    block.setType(Material.AIR);
                    break;
                }
            }
        }
    }
}

