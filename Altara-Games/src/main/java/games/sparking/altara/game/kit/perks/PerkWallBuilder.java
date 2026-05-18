package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Wall Builder</b> – a {@link games.sparking.altara.game.games.bomblobbers.kit.KitWaller}-specific perk.
 *
 * <ul>
 *   <li>Gives the player a "Wall Builder" shovel item (3 charges) at game start.</li>
 *   <li>Right-clicking the shovel places a 3-block-tall × 3-block-wide sandstone wall
 *       directly in front of the player (only in air blocks).</li>
 *   <li>Each use consumes one stack count; when charges are exhausted the item is removed.</li>
 *   <li>All placed blocks are tracked and restored to air when the game ends
 *       ({@link #onUnregister()}).</li>
 * </ul>
 */
public class PerkWallBuilder extends Perk implements Listener {

    /** Height of each placed wall. */
    private static final int WALL_HEIGHT = 3;
    /** How many wall charges are given at the start. */
    private static final int STARTING_CHARGES = 3;
    /** The shovel's inventory slot. */
    private static final int SLOT_SHOVEL = 1;

    private final List<Location> _placedBlocks = new ArrayList<>();

    public PerkWallBuilder() {
        super("Wall Builder", new String[]{
                "§7Right-click shovel §8→ §aplace a sandstone wall",
                "§73 charges"
        });
    }

    @Override
    public void apply(Player player) {
        player.getInventory().setItem(SLOT_SHOVEL, makeShovel(STARTING_CHARGES));
    }

    @Override
    public void remove(Player player) {
        ItemStack slot = player.getInventory().getItem(SLOT_SHOVEL);
        if (slot != null && slot.getType() == Material.WOODEN_SHOVEL)
            player.getInventory().setItem(SLOT_SHOVEL, null);
    }

    @Override
    public void onUnregister() {
        // Restore all blocks placed during the game.
        _placedBlocks.forEach(loc -> loc.getBlock().setType(Material.AIR));
        _placedBlocks.clear();
    }

    // =========================================================================
    // Wall placement
    // =========================================================================

    @EventHandler
    public void onPlaceWall(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack shovel = player.getInventory().getItem(SLOT_SHOVEL);
        if (shovel == null || shovel.getType() != Material.WOODEN_SHOVEL) return;

        event.setCancelled(true);

        int charges = shovel.getAmount();
        if (charges <= 0) {
            player.sendActionBar(Component.text("No wall charges remaining!", NamedTextColor.RED));
            return;
        }

        if (!buildWall(player)) {
            player.sendActionBar(Component.text("No room for a wall here!", NamedTextColor.RED));
            return;
        }

        if (charges == 1) {
            player.getInventory().setItem(SLOT_SHOVEL, new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItem(SLOT_SHOVEL, makeShovel(charges - 1));
        }
        player.updateInventory();
        player.sendActionBar(Component.text("Wall placed! (" + (charges - 1) + " left)", NamedTextColor.GREEN));
    }

    // =========================================================================
    // Geometry
    // =========================================================================

    private boolean buildWall(Player player) {
        Vector dir = player.getLocation().getDirection().setY(0).normalize();
        Vector perp = new Vector(-dir.getZ(), 0, dir.getX());

        Location base = player.getLocation()
                .add(dir.clone().multiply(2.0))
                .subtract(0, 0.5, 0);

        boolean placed = false;
        for (int y = 0; y < WALL_HEIGHT; y++) {
            for (int side = -1; side <= 1; side++) {
                Location spot = base.clone()
                        .add(perp.clone().multiply(side))
                        .add(0, y, 0);
                Block block = spot.getBlock();
                if (block.getType().isAir()) {
                    block.setType(Material.SANDSTONE);
                    _placedBlocks.add(block.getLocation());
                    placed = true;
                }
            }
        }
        return placed;
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static ItemStack makeShovel(int charges) {
        ItemStack item = new ItemStack(Material.WOODEN_SHOVEL, charges);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Wall Builder (" + charges + ")", NamedTextColor.YELLOW));
        meta.lore(List.of(
                Component.text("Right-click to place a sandstone wall.", NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }
}

