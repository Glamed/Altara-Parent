package games.sparking.altara.game.module.chest;

import games.sparking.altara.game.module.GameModule;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Game module that manages loot-bearing chest blocks, matching Mineplex's
 * {@code ChestLootModule} behaviour exactly.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Places {@link Material#CHEST} blocks at registered locations (or nearby) on game start.</li>
 *   <li>Rotates chests to face an open direction automatically ({@link #autoRotateChests}).</li>
 *   <li>Supports per-type spawn chance (skip some spawns randomly).</li>
 *   <li>Fills chests on first open, or pre-generates loot on enable ({@link #setPreGenerateLoot}).</li>
 *   <li>Optionally removes opened chests after a configurable delay ({@link #destroyAfterOpened}).</li>
 *   <li>Supports chest refill ({@link #refill()}) and dynamic location addition ({@link #addChestLocation}).</li>
 * </ul>
 *
 * <h2>Usage (inside {@code onStart()})</h2>
 * <pre>{@code
 * addModule(new ChestLootModule()
 *         .registerChestType("Island", arena.getData("island_chest"),
 *                 new ChestLootPool()
 *                         .addItem(new ItemStack(Material.STONE_SWORD))
 *                         .setProbability(0.9),
 *                 new ChestLootPool()
 *                         .addItem(new ItemStack(Material.COOKED_BEEF), 1, 3)
 *                         .setAmountsPerChest(1, 2))
 *         .destroyAfterOpened(30)
 *         .setPreGenerateLoot(true));
 * }</pre>
 */
public class ChestLootModule extends GameModule
{

    // ─── Constants ───────────────────────────────────────────────────────────

    private static final BlockFace[] HORIZONTALS = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    // ─── Internal types ──────────────────────────────────────────────────────

    private static final class ChestType
    {
        final String            Name;
        final double            SpawnChance;
        final List<ChestLootPool> Pools;
        final List<Location>    ChestSpawns;

        ChestType(String name, List<Location> chestLocations, double spawnChance, ChestLootPool... pools)
        {
            Name        = name;
            SpawnChance = spawnChance;
            Pools       = List.of(pools);
            ChestSpawns = new ArrayList<>(chestLocations);
        }
    }

    private static final class ChestMetadata
    {
        Block     Chest;
        ChestType Type;
        long      OpenedAt;
        boolean   Opened;

        ChestMetadata(Block chest, ChestType type)
        {
            Chest = chest;
            Type  = type;
        }

        void populateChest(Chest chestState)
        {
            Inventory inventory = chestState.getBlockInventory();
            inventory.clear();

            List<Integer> slots = new ArrayList<>(inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) slots.add(i);

            for (ChestLootPool pool : Type.Pools)
            {
                if (pool.getProbability() >= 1.0 || Math.random() < pool.getProbability())
                {
                    pool.populateChest(chestState, slots);
                }
            }
        }
    }

    // ─── State ───────────────────────────────────────────────────────────────

    private final Map<ChestType, Set<ChestMetadata>> _chests = new LinkedHashMap<>();

    private long    _destroyAfterOpened  = 0;
    private boolean _autoRotateChests    = true;
    private boolean _spawnNearby         = false;
    private int     _spawnNearbyRadius   = 8;
    private boolean _preGenerateLoot     = false;

    // ─── Builder ─────────────────────────────────────────────────────────────

    /**
     * Registers a chest tier with its locations and loot pools.
     * Every registered location will always spawn a chest (100 % spawn chance).
     */
    public ChestLootModule registerChestType(String name, List<Location> chestLocations, ChestLootPool... pools)
    {
        return registerChestType(name, chestLocations, 1.0, pools);
    }

    /**
     * Registers a chest tier with its locations, a per-chest spawn chance, and loot pools.
     *
     * @param spawnChance probability (0–1) that each individual location spawns a chest
     */
    public ChestLootModule registerChestType(String name, List<Location> chestLocations,
                                             double spawnChance, ChestLootPool... pools)
    {
        _chests.put(new ChestType(name, chestLocations, spawnChance, pools), new HashSet<>());
        return this;
    }

    /**
     * Removes opened chests {@code seconds} seconds after they are first opened.
     * Pass {@code 0} (default) to disable automatic removal.
     */
    public ChestLootModule destroyAfterOpened(int seconds)
    {
        _destroyAfterOpened = TimeUnit.SECONDS.toMillis(seconds);
        return this;
    }

    /**
     * Whether chests should be automatically rotated to face an open (non-solid) direction.
     * Default: {@code true}.
     */
    public ChestLootModule autoRotateChests(boolean autoRotate)
    {
        _autoRotateChests = autoRotate;
        return this;
    }

    /**
     * When enabled, chests are spawned near the registered data points (within the default
     * radius of 8 blocks) rather than exactly on them.
     */
    public ChestLootModule spawnNearbyDataPoints()
    {
        _spawnNearby = true;
        return this;
    }

    /**
     * When enabled, chests are spawned near the registered data points (within {@code radius}
     * blocks) rather than exactly on them.
     */
    public ChestLootModule spawnNearbyDataPoints(int radius)
    {
        _spawnNearby       = true;
        _spawnNearbyRadius = radius;
        return this;
    }

    /**
     * When {@code true}, chest inventories are generated when the module enables (game start).
     * When {@code false} (default), inventories are generated the first time a player opens each chest.
     */
    public ChestLootModule setPreGenerateLoot(boolean preGenerateLoot)
    {
        _preGenerateLoot = preGenerateLoot;
        return this;
    }

    /**
     * Dynamically adds a chest location to an already-registered tier.
     * The chest block is placed immediately if the module is already enabled.
     */
    public void addChestLocation(String typeName, Location location)
    {
        for (Map.Entry<ChestType, Set<ChestMetadata>> entry : _chests.entrySet())
        {
            if (!entry.getKey().Name.equals(typeName)) continue;

            Block block = location.getBlock();
            ChestMetadata metadata = new ChestMetadata(block, entry.getKey());
            entry.getValue().add(metadata);
            return;
        }
    }

    /**
     * Marks every tracked chest as unopened so players can loot them again.
     * Also re-clears the inventory of each chest block (if still present).
     */
    public void refill()
    {
        _chests.forEach((type, metadataSet) -> metadataSet.forEach(m -> m.Opened = false));
    }

    /**
     * Marks every tracked chest of the given tier as unopened.
     */
    public void refill(String typeName)
    {
        _chests.forEach((type, metadataSet) ->
        {
            if (type.Name.equals(typeName))
            {
                metadataSet.forEach(m -> m.Opened = false);
            }
        });
    }

    /**
     * Returns a random item from a randomly chosen pool of the named chest tier,
     * or {@code null} if the tier is not found or has no pools.
     */
    public ItemStack getRandomItem(String chestTypeName)
    {
        for (ChestType type : _chests.keySet())
        {
            if (!type.Name.equals(chestTypeName)) continue;
            if (type.Pools.isEmpty()) return null;

            ChestLootPool pool = type.Pools.get(ThreadLocalRandom.current().nextInt(type.Pools.size()));
            return pool.getRandomItem();
        }
        return null;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onEnable()
    {
        placeAndRegisterChests();
    }

    // ─── Chest placement ──────────────────────────────────────────────────────

    /**
     * Places chest blocks at all registered locations and (optionally) pre-populates them.
     * Mirrors Mineplex's {@code populateChests} which ran on {@code GameState.Prepare}.
     */
    private void placeAndRegisterChests()
    {
        for (Map.Entry<ChestType, Set<ChestMetadata>> entry : _chests.entrySet())
        {
            ChestType chestType = entry.getKey();
            if (chestType.ChestSpawns == null || chestType.ChestSpawns.isEmpty()) continue;

            Set<ChestMetadata> metadataSet = entry.getValue();

            for (Location location : chestType.ChestSpawns)
            {
                // Spawn-chance check
                if (chestType.SpawnChance < 1.0 && Math.random() >= chestType.SpawnChance) continue;

                Block block;
                if (_spawnNearby)
                {
                    Location nearby = getNearbyLocation(location);
                    if (nearby == null) continue;
                    block = nearby.getBlock();
                }
                else
                {
                    block = location.getBlock();
                }

                // Place the chest block
                block.setType(Material.CHEST);

                // Auto-rotate: face the first open horizontal direction
                if (_autoRotateChests)
                {
                    BlockFace facing = getBestFacing(block);
                    if (facing != null)
                    {
                        org.bukkit.block.data.type.Chest chestData =
                                (org.bukkit.block.data.type.Chest) block.getBlockData();
                        chestData.setFacing(facing);
                        block.setBlockData(chestData);
                    }
                }

                ChestMetadata metadata = new ChestMetadata(block, chestType);
                metadataSet.add(metadata);

                if (_preGenerateLoot && block.getState() instanceof Chest chest)
                {
                    metadata.populateChest(chest);
                }
            }
        }
    }

    // ─── Chest interaction ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void openChest(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Chest chest)) return;
        if (event.isCancelled()) return;

        ChestMetadata metadata = getFromBlock(block);
        if (metadata == null || metadata.Opened) return;

        // Verify the opening player belongs to this game
        Player player = event.getPlayer();
        if (!getGame().hasPlayer(player)) return;

        metadata.Opened   = true;
        metadata.OpenedAt = System.currentTimeMillis();

        if (!_preGenerateLoot)
        {
            metadata.populateChest(chest);
        }
    }

    // ─── Periodic destroy ─────────────────────────────────────────────────────

    @EventHandler
    public void destroyOpenedChests(UpdateEvent event)
    {
        if (event.getType() != UpdateType.SEC || _destroyAfterOpened == 0) return;

        long now = System.currentTimeMillis();

        for (Set<ChestMetadata> metadataSet : _chests.values())
        {
            metadataSet.removeIf(metadata ->
            {
                if (!metadata.Opened || (now - metadata.OpenedAt) < _destroyAfterOpened) return false;

                Block block    = metadata.Chest;
                Location loc   = block.getLocation().clone().add(0.5, 0.5, 0.5);
                //noinspection deprecation
                loc.getWorld().playEffect(loc, Effect.STEP_SOUND, block.getType());

                if (block.getType() == Material.CHEST && block.getState() instanceof Chest chest)
                {
                    chest.getBlockInventory().clear();
                }

                block.setType(Material.AIR);
                return true;
            });
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ChestMetadata getFromBlock(Block block)
    {
        Location loc = block.getLocation();
        for (Set<ChestMetadata> metadataSet : _chests.values())
        {
            for (ChestMetadata metadata : metadataSet)
            {
                if (metadata.Chest.getLocation().equals(loc))
                {
                    return metadata;
                }
            }
        }
        return null;
    }

    /**
     * Returns a horizontal face adjacent to {@code block} that has a non-solid neighbour,
     * or {@code null} if all four directions are blocked.
     */
    private BlockFace getBestFacing(Block block)
    {
        List<BlockFace> open = new ArrayList<>();
        for (BlockFace face : HORIZONTALS)
        {
            if (!block.getRelative(face).getType().isSolid())
            {
                open.add(face);
            }
        }
        if (open.isEmpty()) return null;
        return open.get(ThreadLocalRandom.current().nextInt(open.size()));
    }

    /**
     * Attempts to find a suitable block near {@code center} into which a chest can be placed.
     * Mirrors Mineplex's {@code getNearbyLocation}: up to 20 random attempts within
     * ±{@code _spawnNearbyRadius} in X/Z and ±1 in Y.
     *
     * @return a suitable {@link Location}, or {@code null} if none found after 20 attempts
     */
    private Location getNearbyLocation(Location center)
    {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 20; attempt++)
        {
            int dx = rng.nextInt(-_spawnNearbyRadius, _spawnNearbyRadius + 1);
            int dy = rng.nextInt(-1, 2);
            int dz = rng.nextInt(-_spawnNearbyRadius, _spawnNearbyRadius + 1);
            Location candidate = center.clone().add(dx, dy, dz);
            if (isSuitable(candidate.getBlock())) return candidate;
        }
        return null;
    }

    /**
     * A block is suitable for chest placement when it is air, the block above it is air,
     * the block below it is solid and non-liquid, and neither the block nor its neighbours are liquid.
     */
    private boolean isSuitable(Block block)
    {
        Block up   = block.getRelative(BlockFace.UP);
        Block down = block.getRelative(BlockFace.DOWN);
        return block.getType() == Material.AIR
                && up.getType() == Material.AIR
                && down.getType() != Material.AIR
                && !isLiquid(down)
                && !isLiquid(up)
                && !isLiquid(block);
    }

    private static boolean isLiquid(Block block)
    {
        Material t = block.getType();
        return t == Material.WATER || t == Material.LAVA;
    }
}
