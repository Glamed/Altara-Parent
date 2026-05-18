package games.sparking.altara.game.module.generator;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * Defines what a {@link Generator} produces and how it looks.
 *
 * <h2>Creating a type</h2>
 * <pre>{@code
 * GeneratorType DIAMOND = new GeneratorType(
 *         new ItemStack(Material.DIAMOND),
 *         30_000L,           // 30-second respawn rate
 *         "Diamond",
 *         ChatColor.AQUA,
 *         Color.AQUA,
 *         true               // flash name
 * );
 * }</pre>
 */
public class GeneratorType {

    private final ItemStack  itemStack;
    private final long       spawnRate;
    private final String     name;
    private final ChatColor  colour;
    private final boolean    flashName;
    private final Color      fireworkColor;

    public GeneratorType(ItemStack itemStack, long spawnRate, String name,
                         ChatColor chatColour, Color fireworkColor, boolean flashName) {
        this.itemStack      = itemStack;
        this.spawnRate      = spawnRate;
        this.name           = name;
        this.colour         = chatColour;
        this.flashName      = flashName;
        this.fireworkColor  = fireworkColor;
    }

    // -------------------------------------------------------------------------
    // Called by Generator
    // -------------------------------------------------------------------------

    /** Plays the collection effect, resets the base block, and gives the item to the player. */
    final void collect(Generator generator, Player player) {
        playFirework(generator.getLocation());
        generator.getBlock().setType(Material.IRON_BLOCK);
        collect(player);
    }

    /** Spawns an ArmorStand helmet display at the generator location. */
    final ArmorStand spawnHolder(Generator generator) {
        Location loc = generator.getLocation();
        ArmorStand holder = loc.getWorld().spawn(loc, ArmorStand.class);

        holder.setGravity(false);
        holder.setVisible(false);
        holder.setHelmet(itemStack.clone());
        holder.setRemoveWhenFarAway(false);

        playFirework(loc);
        generator.getBlock().setType(Material.GOLD_BLOCK);

        return holder;
    }

    // -------------------------------------------------------------------------
    // Public overrideable
    // -------------------------------------------------------------------------

    /** Gives the generated item to the player. Override to customise delivery. */
    public void collect(Player player) {
        player.getInventory().addItem(itemStack.clone());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public ItemStack  getItemStack() { return itemStack;  }
    public long       getSpawnRate() { return spawnRate;  }
    public String     getName()      { return name;       }
    public ChatColor  getColour()    { return colour;     }
    public boolean    isFlashName()  { return flashName;  }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void playFirework(Location location) {
        Firework fw = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(Type.BURST)
                .withColor(fireworkColor)
                .build());
        fw.setFireworkMeta(meta);
        fw.detonate();
    }
}

