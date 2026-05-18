package games.sparking.altara.game.module.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A single resource generator that periodically spawns an item (displayed on an {@link ArmorStand})
 * which is automatically collected by nearby players.
 *
 * <p>Generators are registered with {@link GeneratorModule#addGenerator(Generator)} and are
 * ticked by the module — you do not need to call any update methods yourself.
 */
public class Generator {

    private static final int    COLLECT_RADIUS      = 2;
    private static final float  ROTATION_DELTA_YAW  = 10f;

    private final GeneratorType type;
    private final Location      location;
    private final Block         block;

    private ArmorStand holder;
    private long       lastCollect;
    private boolean    colourTick = true;

    /**
     * @param type     what item the generator produces
     * @param location the centre of the generator (the ArmorStand spawns here)
     */
    public Generator(GeneratorType type, Location location) {
        this.type     = type;
        this.location = location.clone().subtract(0, 0.5, 0);
        this.block    = location.getBlock().getRelative(BlockFace.DOWN);
    }

    // -------------------------------------------------------------------------
    // Tick methods (called by GeneratorModule)
    // -------------------------------------------------------------------------

    /** Checks whether a nearby player can collect the item and fires the collect logic. */
    public void checkCollect() {
        if (holder == null) return;

        List<Player> nearby = location.getWorld().getNearbyPlayers(location, COLLECT_RADIUS).stream().toList();
        if (nearby.isEmpty()) return;

        Player player = nearby.get(0);
        type.collect(this, player);
        holder.remove();
        holder = null;
        setLastCollect();
        Bukkit.getPluginManager().callEvent(new GeneratorCollectEvent(player, this));
    }

    /** Spawns the ArmorStand holder if the spawn rate has elapsed. */
    public void checkSpawn() {
        if (holder != null) return;
        if (System.currentTimeMillis() - lastCollect < type.getSpawnRate()) return;
        holder = type.spawnHolder(this);
    }

    /** Rotates the holder by {@value #ROTATION_DELTA_YAW}° on each call. */
    public void animateHolder() {
        if (holder == null) return;
        Location loc = holder.getLocation();
        holder.setRotation(loc.getYaw() + ROTATION_DELTA_YAW, loc.getPitch());
    }

    /** Updates the custom name colour flash. */
    public void updateName() {
        if (holder == null) return;

        if (!holder.isCustomNameVisible()) {
            holder.setCustomNameVisible(true);
        }

        if (type.isFlashName()) {
            colourTick = !colourTick;
        }

        holder.customName(net.kyori.adventure.text.Component.text(
                (colourTick ? type.getColour() : org.bukkit.ChatColor.WHITE) + "" + org.bukkit.ChatColor.BOLD + type.getName()));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public GeneratorType getType()    { return type;     }
    public ArmorStand    getHolder()  { return holder;   }
    public Location      getLocation(){ return location.clone(); }
    public Block         getBlock()   { return block;    }

    /** Resets the spawn timer; call at game start so the first spawn is delayed. */
    public void setLastCollect() {
        this.lastCollect = System.currentTimeMillis();
    }

    /** Returns milliseconds until the next spawn, or 0 if ready. */
    public long getTimeUntilSpawn() {
        long diff = (lastCollect + type.getSpawnRate()) - System.currentTimeMillis();
        return Math.max(0, diff);
    }
}

