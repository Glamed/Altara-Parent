package games.sparking.altara.game.games.skywars;

import games.sparking.altara.game.games.skywars.kit.*;
import games.sparking.altara.game.games.skywars.module.ZombieGuardianModule;
import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.module.CombatTrackerModule;
import games.sparking.altara.game.module.MapCrumbleModule;
import games.sparking.altara.game.module.ThrowableTNTModule;
import games.sparking.altara.game.module.chest.ChestLootModule;
import games.sparking.altara.game.module.chest.ChestLootPool;
import games.sparking.altara.game.module.compass.CompassModule;
import games.sparking.altara.game.module.generator.Generator;
import games.sparking.altara.game.module.generator.GeneratorModule;
import games.sparking.altara.game.module.generator.GeneratorType;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.team.GameTeam;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import games.sparking.altara.world.AltaraWorld;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * <h1>BaseSkyWars</h1>
 *
 * <p>Shared logic for all SkyWars variants (Solo and Team).
 * Each instance is fully isolated — event handlers verify the acting player is
 * in <em>this</em> session and block events verify the block belongs to
 * <em>this</em> session's arena world.
 *
 * <h2>WorldConfig.dat keys expected</h2>
 * <ul>
 *   <li>{@code TEAM_NAME:Players} (solo) or {@code TEAM_NAME:Red / Blue / …} (team)
 *       — spawn locations per player/team</li>
 *   <li>{@code DATA_NAME:island_chest}    — island (low-tier) chest locations</li>
 *   <li>{@code DATA_NAME:connector_chest} — connector (mid-tier) chest locations</li>
 *   <li>{@code DATA_NAME:middle_chest}    — centre (high-tier) chest locations</li>
 *   <li>{@code DATA_NAME:zombie_spawn}    — zombie guardian spawn locations</li>
 * </ul>
 */
@Getter
public abstract class BaseSkyWars extends Game {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final long CRUMBLE_DELAY_MS = 100_000L; // 100 seconds

    // =========================================================================
    // Constructor
    // =========================================================================

    protected BaseSkyWars() {
        getKitManager().registerKit(new KitIce(this));
        getKitManager().registerKit(new KitFire(this));
        getKitManager().registerKit(new KitAir(this));
        getKitManager().registerKit(new KitMetal(this));
        getKitManager().registerKit(new KitEarth(this));
    }

    // =========================================================================
    // Metadata
    // =========================================================================

    @Override public String getName()        { return "SkyWars"; }
    @Override public String getDescription() { return "Loot, build, and fight on sky islands — last one standing wins!"; }
    @Override public int    getMinPlayers()  { return 2; }
    @Override public int    getMaxPlayers()  { return 12; }
    @Override protected String getMapType()  { return "skywars"; }

    // =========================================================================
    // Game lifecycle
    // =========================================================================

    @Override
    protected void onRecruit() {
        broadcast(ChatColor.AQUA + "SkyWars — choose your kit!");
        super.onRecruit();
    }

    /**
     * Spawns players at the given spawn points, applies kits, and registers modules.
     * Called from the concrete subclass's {@code onStart()}.
     */
    protected void startGame(List<Location> spawns) {
        List<Location> shuffled = new ArrayList<>(spawns);
        Collections.shuffle(shuffled);

        int idx = 0;
        for (GamePlayer gp : getPlayers().values()) {
            Player p = gp.getPlayer();
            if (p == null) continue;
            if (!shuffled.isEmpty()) {
                p.teleport(shuffled.get(idx % shuffled.size()));
                idx++;
            }
            p.getInventory().clear();
            getKitManager().applyKit(p);
        }

        setupModules();
        broadcast(ChatColor.AQUA + "SkyWars has begun! " + ChatColor.GRAY + "Loot, build, and fight!");
    }

    // =========================================================================
    // Team helpers (usable by TeamSkyWars even though we extend Game directly)
    // =========================================================================

    /**
     * Convenience method to create and register a team within this game.
     * Mirrors the helper in {@link games.sparking.altara.game.impl.TeamGame}.
     */
    protected GameTeam createTeam(String id, String name, games.sparking.altara.game.team.TeamColor color) {
        return addTeam(new GameTeam(id, name, color));
    }

    /**
     * Distributes players evenly across the registered teams.
     * Mirrors the helper in {@link games.sparking.altara.game.impl.TeamGame}.
     */
    protected void distributePlayersEvenly() {
        List<GameTeam> teamList = getTeams();
        if (teamList.isEmpty()) return;
        int i = 0;
        for (GamePlayer gp : getPlayers().values()) {
            teamList.get(i++ % teamList.size()).addPlayer(gp);
        }
    }

    // =========================================================================
    // Module references (exposed for scoreboard, etc.)
    // =========================================================================

    /**
     * -- GETTER --
     * Returns the
     *  for this session, or
     *  if none was created.
     */
    private GeneratorModule generatorModule;

    // =========================================================================
    // Module setup
    // =========================================================================

    private void setupModules() {
        AltaraWorld arena = getArenaWorld();

        // ── Compass (track nearest enemy; also given to alive players) ──────
        addModule(new CompassModule().setGiveCompassToAlive(true));

        // ── Combat Tracker (kills / assists per session) ─────────────────────
        addModule(new CombatTrackerModule());

        // ── Map crumble (after 100 s) ────────────────────────────────────────
        addModule(new MapCrumbleModule(CRUMBLE_DELAY_MS)
                .blocksPerTick(5)
                .startMessage(ChatColor.RED + "" + ChatColor.BOLD + "The world begins to crumble!"));

        // ── Zombie Guardians ─────────────────────────────────────────────────
        if (arena != null) {
            List<Location> zombieSpawns = arena.getData("zombie_spawn");
            if (!zombieSpawns.isEmpty()) {
                addModule(new ZombieGuardianModule(zombieSpawns));
            }
        }

        // ── Throwable-TNT generator ──────────────────────────────────────────
        if (arena != null) {
            List<Location> tntLocs = arena.getData("tnt_generator");
            if (!tntLocs.isEmpty()) {
                ThrowableTNTModule tntModule = addModule(new ThrowableTNTModule()
                        .setThrowAndDrop(true)
                        .setThrowStrength(1.6f));

                ItemStack tntItem = tntModule.getTntItem().clone();
                tntItem.setAmount(2);

                generatorModule = addModule(new GeneratorModule()
                        .addGenerator(new Generator(
                                new GeneratorType(
                                        tntItem,
                                        30_000L,           // 30-second respawn
                                        "Throwable TNT",
                                        ChatColor.RED,
                                        Color.RED,
                                        true),
                                tntLocs.get(0))));
            }
        }

        // ── Chest loot ───────────────────────────────────────────────────────
        if (arena != null) {
            addModule(buildChestLoot(arena));
        }
    }

    private ChestLootModule buildChestLoot(AltaraWorld arena) {
        ChestLootModule module = new ChestLootModule();

        // Island chests (low-tier) — keyed as "BROWN" in WorldConfig.dat
        List<Location> islandChests = arena.getData("BROWN");
        if (!islandChests.isEmpty()) {
            module.registerChestType("Island", islandChests,
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.GOLDEN_HELMET))
                            .addItem(new ItemStack(Material.GOLDEN_CHESTPLATE))
                            .addItem(new ItemStack(Material.GOLDEN_LEGGINGS))
                            .addItem(new ItemStack(Material.GOLDEN_BOOTS))
                            .setAmountsPerChest(2, 3),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.STONE_SWORD))
                            .addEnchantment(Enchantment.SHARPNESS, 1)
                            .setEnchantmentRarity(0.5),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.STONE_AXE))
                            .addItem(new ItemStack(Material.STONE_SHOVEL))
                            .setProbability(0.8),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.BOW))
                            .setProbability(0.15),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.COOKED_BEEF), 1, 3)
                            .addItem(new ItemStack(Material.COOKED_CHICKEN), 1, 3)
                            .addItem(new ItemStack(Material.COOKED_COD), 1, 3)
                            .setAmountsPerChest(1, 2)
                            .setProbability(0.8),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.OAK_LOG), 16, 32)
                            .addItem(new ItemStack(Material.COBBLESTONE), 16, 32)
                            .setProbability(0.9),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.SNOWBALL), 2, 5)
                            .addItem(new ItemStack(Material.EGG), 2, 5)
                            .setProbability(0.4)
            );
        }

        // Connector chests (mid-tier) — keyed as "GRAY" in WorldConfig.dat
        List<Location> connectorChests = arena.getData("GRAY");
        if (!connectorChests.isEmpty()) {
            module.registerChestType("Connector", connectorChests,
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.CHAINMAIL_HELMET))
                            .addItem(new ItemStack(Material.CHAINMAIL_CHESTPLATE))
                            .addItem(new ItemStack(Material.CHAINMAIL_LEGGINGS))
                            .addItem(new ItemStack(Material.CHAINMAIL_BOOTS))
                            .addEnchantment(Enchantment.PROTECTION, 1)
                            .addEnchantment(Enchantment.PROJECTILE_PROTECTION, 1)
                            .setEnchantmentRarity(0.6)
                            .setAmountsPerChest(1, 2),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.STONE_SWORD))
                            .addEnchantment(Enchantment.SHARPNESS, 1)
                            .setProbability(0.8),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.IRON_AXE))
                            .addItem(new ItemStack(Material.IRON_PICKAXE))
                            .addItem(new ItemStack(Material.FISHING_ROD))
                            .setProbability(0.7),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.BOW))
                            .setProbability(0.25),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.COOKED_BEEF), 1, 3)
                            .addItem(new ItemStack(Material.COOKED_CHICKEN), 1, 3)
                            .addItem(new ItemStack(Material.COOKED_COD), 1, 3)
                            .setAmountsPerChest(1, 2)
                            .setProbability(0.8),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.OAK_LOG), 16, 32)
                            .addItem(new ItemStack(Material.COBBLESTONE), 16, 32)
                            .addItem(new ItemStack(Material.GLASS), 16, 32)
                            .setProbability(0.9),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.ARROW), 4, 8)
                            .addItem(new ItemStack(Material.SNOWBALL), 2, 5)
                            .addItem(new ItemStack(Material.EGG), 2, 5)
                            .addItem(new ItemStack(Material.LAVA_BUCKET))
                            .addItem(new ItemStack(Material.WATER_BUCKET))
                            .addItem(new ItemStack(Material.ENDER_PEARL), 1, 2)
                            .setAmountsPerChest(1, 2)
            );
        }

        // Middle chests (high-tier) — keyed as "RED", "BLACK", and "LIME" in WorldConfig.dat
        List<Location> middleChests = new ArrayList<>();
        middleChests.addAll(arena.getData("RED"));
        middleChests.addAll(arena.getData("BLACK"));
        middleChests.addAll(arena.getData("LIME"));
        if (!middleChests.isEmpty()) {
            module.registerChestType("Middle", middleChests,
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.IRON_HELMET))
                            .addItem(new ItemStack(Material.IRON_CHESTPLATE))
                            .addItem(new ItemStack(Material.IRON_LEGGINGS))
                            .addItem(new ItemStack(Material.IRON_BOOTS))
                            .addEnchantment(Enchantment.PROTECTION, 2)
                            .addEnchantment(Enchantment.PROJECTILE_PROTECTION, 2)
                            .setAmountsPerChest(1, 2),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.IRON_SWORD))
                            .addEnchantment(Enchantment.SHARPNESS, 1)
                            .addEnchantment(Enchantment.KNOCKBACK, 1)
                            .setProbability(0.4),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.DIAMOND_SWORD), 30)
                            .addItem(new ItemStack(Material.DIAMOND), 1, 3)
                            .setProbability(0.4),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.IRON_AXE))
                            .addItem(new ItemStack(Material.IRON_PICKAXE))
                            .addItem(new ItemStack(Material.FISHING_ROD))
                            .setProbability(0.8),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.BOW))
                            .setProbability(0.35),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.COOKED_BEEF), 1, 3)
                            .addItem(new ItemStack(Material.COOKED_CHICKEN), 1, 3)
                            .addItem(new ItemStack(Material.COOKED_COD), 1, 3)
                            .setAmountsPerChest(1, 2)
                            .setProbability(0.8),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.OAK_LOG), 16, 32)
                            .addItem(new ItemStack(Material.COBBLESTONE), 16, 32)
                            .addItem(new ItemStack(Material.GLASS), 16, 32)
                            .setProbability(0.9),
                    new ChestLootPool()
                            .addItem(new ItemStack(Material.EXPERIENCE_BOTTLE), 5, 10)
                            .addItem(new ItemStack(Material.ARROW), 6, 10)
                            .addItem(new ItemStack(Material.SNOWBALL), 2, 5)
                            .addItem(new ItemStack(Material.EGG), 2, 5)
                            .addItem(new ItemStack(Material.LAVA_BUCKET))
                            .addItem(new ItemStack(Material.WATER_BUCKET))
                            .addItem(new ItemStack(Material.ENDER_PEARL), 1, 2)
                            .addItem(new ItemStack(Material.MUSHROOM_STEW))
                            .setAmountsPerChest(2, 3)
            );
        }

        return module;
    }

    // =========================================================================
    // End / cleanup
    // =========================================================================

    @Override
    protected void onEnd() {
        super.onEnd(); // schedules destroy after 5s
    }

    @Override
    protected void onDead() {
        super.onDead(); // unloads the arena world
    }

    // =========================================================================
    // Generator timer display
    // =========================================================================

    /** Sends alive players a periodic TNT‑generator countdown message. */
    @EventHandler
    public void onUpdateSecBase(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC || !isLive()) return;
        if (generatorModule == null || generatorModule.getGenerators().isEmpty()) return;

        Generator gen = generatorModule.getGenerators().get(0);
        long millis = gen.getTimeUntilSpawn();

        String status = millis > 0
                ? ChatColor.YELLOW + formatTime(millis)
                : ChatColor.GREEN + "" + ChatColor.BOLD + "Ready to collect!";

        String msg = ChatColor.GRAY + "Throwable TNT: " + status;

        for (GamePlayer gp : getPlayers().values()) {
            if (!gp.isAlive()) continue;
            Player p = gp.getPlayer();
            if (p != null) p.sendMessage(msg);
        }
    }

    private static String formatTime(long millis) {
        long secs = millis / 1000;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    // =========================================================================
    // Void boundary €" session-scoped
    // =========================================================================

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isLive()) return;
        Player player = event.getPlayer();
        if (!hasPlayer(player)) return;
        GamePlayer gp = getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isAlive()) return;

        Location to = event.getTo();
        if (to == null) return;

        AltaraWorld arena = getArenaWorld();
        int minY = (arena != null) ? arena.getMin().getBlockY() : 0;
        if (to.getY() < minY - 15) {
            eliminatePlayer(player);
        }
    }

    // =========================================================================
    // Block events — restricted to this session's arena world
    // =========================================================================

    /**
     * Returns {@code true} if the block's world is this game's arena world.
     * This ensures no events from other sessions (different worlds) are processed.
     */
    private boolean isArenaWorld(World world) {
        AltaraWorld arena = getArenaWorld();
        return arena != null && arena.getWorld().equals(world);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (!isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!isArenaWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isLive()) return;
        Player player = event.getPlayer();
        if (!hasPlayer(player)) return;
        if (event.isCancelled()) return;

        AltaraWorld arena = getArenaWorld();
        org.bukkit.block.Block block = event.getBlock();
        Material material = block.getType();

        if (arena != null && block.getLocation().getY() >= arena.getMax().getBlockY() - 3) {
            player.sendMessage(ChatColor.RED + "You cannot build this high.");
            event.setCancelled(true);
            return;
        }

        if (material == Material.CHEST
                || material == Material.PISTON
                || material == Material.STICKY_PISTON
                || material == Material.HOPPER) {
            player.sendMessage(ChatColor.RED + "You cannot place that block.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreakBonusDrops(BlockBreakEvent event) {
        if (!isLive()) return;
        Player player = event.getPlayer();
        if (!hasPlayer(player)) return;
        if (!isArenaWorld(event.getBlock().getWorld())) return;

        event.setExpToDrop(0);

        org.bukkit.block.Block block = event.getBlock();
        ItemStack toDrop = null;
        Random rng = new Random();

        switch (block.getType()) {
            case COBWEB               -> toDrop = new ItemStack(Material.STRING, 1 + rng.nextInt(2));
            case GRAVEL               -> toDrop = new ItemStack(Material.FLINT,  1 + rng.nextInt(3));
            case IRON_ORE, DEEPSLATE_IRON_ORE -> toDrop = new ItemStack(Material.IRON_INGOT);
            default -> {}
        }

        if (toDrop != null) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(
                    block.getLocation().add(0.5, 0.5, 0.5), toDrop);
        }
    }

    @EventHandler
    public void onPlayerEnchant(EnchantItemEvent event) {
        if (!isLive()) return;
        Player player = event.getEnchanter();
        if (!hasPlayer(player)) return;

        if (event.getEnchantsToAdd().containsKey(Enchantment.FIRE_ASPECT)) {
            player.sendMessage(ChatColor.RED + "You cannot enchant with Fire Aspect.");
            event.setCancelled(true);
        }
    }
}
