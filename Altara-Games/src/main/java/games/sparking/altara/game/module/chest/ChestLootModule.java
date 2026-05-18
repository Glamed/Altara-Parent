package games.sparking.altara.game.module.chest;

import games.sparking.altara.game.module.GameModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Game module that pre-fills chests with randomised loot when the game starts.
 *
 * <h2>Usage (inside {@code onStart()})</h2>
 * <pre>{@code
 * addModule(new ChestLootModule()
 *         .addChestType("Island", arenaWorld.getData("island_chest"),
 *                 new ChestLootPool()
 *                         .addItem(new ItemStack(Material.STONE_SWORD))
 *                         .setProbability(0.9),
 *                 new ChestLootPool()
 *                         .addItem(new ItemStack(Material.COOKED_BEEF), 1, 3)
 *                         .setAmountsPerChest(1, 2))
 *         .addChestType("Middle", arenaWorld.getData("middle_chest"), ...));
 * }</pre>
 *
 * <p>Chests are filled once when {@link #onEnable()} is called (i.e. when the game
 * transitions to {@link games.sparking.altara.game.GameState#Live}).
 */
public class ChestLootModule extends GameModule {

    // -------------------------------------------------------------------------
    // Internal record
    // -------------------------------------------------------------------------

    private record ChestType(String name, List<Location> locations, List<ChestLootPool> pools) {}

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final List<ChestType> chestTypes = new ArrayList<>();
    private final Random          rng        = new Random();

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Registers a tier of chests with the locations they occupy and the loot pools to draw from.
     *
     * @param name      human-readable tier name (used in log messages)
     * @param locations block locations of each chest in this tier
     * @param pools     one or more loot pools; all pools are sampled per chest
     * @return {@code this} for chaining
     */
    public ChestLootModule addChestType(String name, List<Location> locations, ChestLootPool... pools) {
        chestTypes.add(new ChestType(name, List.copyOf(locations), List.of(pools)));
        return this;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onEnable() {
        fillAllChests();
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void fillAllChests() {
        for (ChestType type : chestTypes) {
            int filled = 0;
            for (Location loc : type.locations()) {
                if (fillChest(loc, type.pools())) filled++;
            }
            getGame().getArenaWorld(); // just reference to confirm world is alive
        }
    }

    /**
     * Fills a single chest block with randomised loot from the given pools.
     *
     * @param loc   world location of the chest block
     * @param pools loot pools to draw from
     * @return {@code true} if the block was actually a chest and was filled
     */
    private boolean fillChest(Location loc, List<ChestLootPool> pools) {
        Block block = loc.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return false;
        }
        if (!(block.getState() instanceof Chest chest)) {
            return false;
        }

        Inventory inv = chest.getInventory();
        inv.clear();

        // Collect all items from all pools
        List<ItemStack> loot = new ArrayList<>();
        for (ChestLootPool pool : pools) {
            loot.addAll(pool.generate(rng));
        }

        // Shuffle and place into random slots to make chests feel organic
        Collections.shuffle(loot, rng);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, rng);

        int slotIdx = 0;
        for (ItemStack item : loot) {
            if (item == null || item.getType().isAir()) continue;
            if (slotIdx >= slots.size()) break;
            inv.setItem(slots.get(slotIdx++), item);
        }

        return true;
    }
}

