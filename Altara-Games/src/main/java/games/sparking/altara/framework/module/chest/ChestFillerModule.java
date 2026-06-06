package games.sparking.altara.framework.module.chest;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameModule;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.annotation.GameEvent;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Shared {@link GameModule} that populates chests using a configurable {@link LootTable}.
 *
 * <h3>Two fill strategies</h3>
 * <ol>
 *   <li><b>Eager (default)</b> — call {@link #fillChests(Collection)} or
 *       {@link #fillChest(Block)} explicitly from your game's {@link Game#start()}
 *       to fill every chest before players enter the map.</li>
 *   <li><b>Lazy (fill-on-open)</b> — pass {@code fillOnOpen = true} to the
 *       constructor.  Chests are then filled the first time a playing player
 *       opens them; unopened chests remain empty until touched.</li>
 * </ol>
 *
 * <p>Regardless of the strategy, each chest location is tracked in an internal
 * set so it is filled at most once per game session.
 * Call {@link #reset()} in your game's {@link Game#stop()} if you reuse the
 * same instance across sessions.
 *
 * <h3>Wiring example (eager)</h3>
 * <pre>
 * public class SkyWarsSolosGame extends SoloGame {
 *
 *     private final ChestFillerModule chests =
 *             new ChestFillerModule(SkyWarsSolosGame.LOOT_TABLE);
 *
 *     {@literal @}Override
 *     public List{@literal <GameModule>} modules() {
 *         return List.of(chests, new SpectatorModule());
 *     }
 *
 *     {@literal @}Override
 *     public void start() {
 *         chests.fillChests(getChestLocations());
 *     }
 * }
 * </pre>
 *
 * <h3>Wiring example (lazy / fill-on-open)</h3>
 * <pre>
 * private final ChestFillerModule chests =
 *         new ChestFillerModule(MY_LOOT_TABLE, true);
 * </pre>
 */
public class ChestFillerModule extends GameModule {

    /** The loot table used to generate chest contents. */
    @Getter
    private final LootTable lootTable;

    /**
     * When {@code true}, chests are filled the first time a playing player
     * interacts with them rather than all at once at game start.
     */
    private final boolean fillOnOpen;

    /** Tracks which locations have already been filled this session. */
    private final Set<Location> filledLocations = new HashSet<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a module that uses <em>eager</em> filling.
     * You must call {@link #fillChests(Collection)} yourself from {@link Game#start()}.
     *
     * @param lootTable the loot table to use for all chest fills
     */
    public ChestFillerModule(LootTable lootTable) {
        this(lootTable, false);
    }

    /**
     * @param lootTable  the loot table to use for all chest fills
     * @param fillOnOpen when {@code true}, chests are filled lazily the first
     *                   time a playing player opens them
     */
    public ChestFillerModule(LootTable lootTable, boolean fillOnOpen) {
        if (lootTable == null) throw new IllegalArgumentException("lootTable must not be null");
        this.lootTable  = lootTable;
        this.fillOnOpen = fillOnOpen;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void cleanup() {
        reset();
    }

    /**
     * Clears the set of already-filled locations.
     * Call this between game sessions if you reuse the same module instance.
     */
    public void reset() {
        filledLocations.clear();
    }

    // -------------------------------------------------------------------------
    // Eager fill API
    // -------------------------------------------------------------------------

    /**
     * Fills every chest at the given locations, skipping locations that have
     * already been filled this session.
     *
     * @param locations chest block locations; non-chest blocks are silently ignored
     */
    public void fillChests(Collection<Location> locations) {
        for (Location location : locations) {
            fillChest(location.getBlock());
        }
    }

    /**
     * Fills a single chest block.
     *
     * <p>If the block is not a {@link Chest} or has already been filled this
     * session, this method is a no-op.
     *
     * @param block the block to fill
     */
    public void fillChest(Block block) {
        if (!isChest(block)) return;
        if (!filledLocations.add(block.getLocation())) return; // already filled

        Chest chest = (Chest) block.getState();
        populateInventory(chest.getBlockInventory());
    }

    // -------------------------------------------------------------------------
    // Lazy fill — fill-on-open event handler
    // -------------------------------------------------------------------------

    /**
     * When {@link #fillOnOpen} is enabled, intercepts a player's chest-open
     * interaction and fills the chest before the client sees its contents.
     */
    @GameEvent(value = PlayerInteractEvent.class, states = {GameState.PLAYING})
    public void onChestOpen(PlayerInteractEvent event, Game game, Player player, GameState state) {
        if (!fillOnOpen) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !isChest(clicked)) return;

        if (filledLocations.add(clicked.getLocation())) {
            Chest chest = (Chest) clicked.getState();
            populateInventory(chest.getBlockInventory());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Writes generated loot into random inventory slots, leaving others empty. */
    private void populateInventory(Inventory inventory) {
        inventory.clear();

        List<ItemStack> loot = lootTable.generate();
        int size = inventory.getSize();

        List<Integer> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) slots.add(i);
        Collections.shuffle(slots);

        int placed = 0;
        for (ItemStack item : loot) {
            if (placed >= slots.size()) break;
            inventory.setItem(slots.get(placed++), item);
        }
    }

    private static boolean isChest(Block block) {
        return block != null
                && (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)
                && block.getState() instanceof Chest;
    }

    /** Returns an unmodifiable view of all locations that have been filled. */
    public Set<Location> getFilledLocations() {
        return Collections.unmodifiableSet(filledLocations);
    }
}

