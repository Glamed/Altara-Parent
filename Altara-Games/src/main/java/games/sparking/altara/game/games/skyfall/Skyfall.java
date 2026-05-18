package games.sparking.altara.game.games.skyfall;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.GameState;
import games.sparking.altara.game.games.skyfall.event.PlayerBoostRingEvent;
import games.sparking.altara.game.games.skyfall.kit.*;
import games.sparking.altara.game.impl.SoloGame;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import games.sparking.altara.world.MapLoader;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * <h1>Skyfall</h1>
 *
 * <p>An elytra-based aerial combat game. Players fly on sky islands that crumble over time.
 * Fly through boost rings for speed bursts, loot chests on islands, and be the last one gliding.
 *
 * <h2>WorldConfig.dat notes</h2>
 * <ul>
 *   <li>{@code TEAM_NAME:Players} — individual spawn points (elevated, above islands)</li>
 *   <li>{@code DATA_NAME:island_chest} — chest locations on islands</li>
 *   <li>{@code DATA_NAME:boost_ring} — center location of each booster ring (one per entry)</li>
 *   <li>{@code DATA_NAME:supply_drop} — supply drop location</li>
 * </ul>
 */
public class Skyfall extends SoloGame {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final double RING_DETECT_RADIUS  = 3.0;
    private static final float  RING_BOOST_STRENGTH = 3.25f;

    private static final long CRUMBLE_DELAY_MS     = 2 * 60 * 1000;          // 2 minutes
    private static final long SUPPLY_DROP_DELAY_MS  = 5 * 60 * 1000;          // 5 minutes
    private static final long SUPPLY_DROP_WARN_MS   = 4 * 60 * 1000 + 30_000; // 4m 30s

    // =========================================================================
    // State
    // =========================================================================

    private boolean crumbleStarted       = false;
    private boolean supplyDropAnnounced  = false;
    private boolean supplyDropActive     = false;

    /** Players who died by void fall — to give context-specific broadcast in onPlayerEliminated. */
    private final Set<UUID> voidDeaths = new HashSet<>();

    private final List<Location>              ringCenters    = new ArrayList<>();
    private final Map<UUID, Map<Location, Long>> ringCooldowns = new HashMap<>();
    private Location supplyDropLocation;

    // =========================================================================
    // Constructor
    // =========================================================================

    public Skyfall() {
        getKitManager().registerKit(new KitAeronaught(this));
        getKitManager().registerKit(new KitBooster(this));
        getKitManager().registerKit(new KitDeadeye(this));
        getKitManager().registerKit(new KitJouster(this));
        getKitManager().registerKit(new KitSpeeder(this));
        getKitManager().registerKit(new KitStunner(this));
    }

    // =========================================================================
    // Metadata
    // =========================================================================

    @Override public String getName()        { return "Skyfall"; }
    @Override public String getDescription() { return "Fly on elytra, fight enemies, and be the last one gliding!"; }
    @Override public int    getMinPlayers()  { return 2; }
    @Override public int    getMaxPlayers()  { return 12; }

    // =========================================================================
    // onLoad
    // =========================================================================

    @Override
    protected void onLoad() {
        if (!MapLoader.hasAnyMap("skyfall")) {
            AltaraPaper.getPaperInstance().getLogger()
                    .warning("[Skyfall] No maps found — starting without a world.");
            setState(GameState.Recruit);
            return;
        }

        MapLoader.loadRandom("skyfall", getShortId())
                .thenAcceptAsync(world -> {
                    setArenaWorld(world);
                    setState(GameState.Recruit);
                }, r -> Bukkit.getScheduler().runTask(AltaraPaper.getPaperInstance(), r))
                .exceptionally(err -> {
                    AltaraPaper.getPaperInstance().getLogger()
                            .severe("[Skyfall] Map load failed: " + err.getMessage());
                    Bukkit.getScheduler().runTask(AltaraPaper.getPaperInstance(),
                            () -> setState(GameState.Recruit));
                    return null;
                });
    }

    // =========================================================================
    // onRecruit
    // =========================================================================

    @Override
    protected void onRecruit() {
        broadcast(ChatColor.AQUA + "Skyfall — choose a kit!");
        if (arenaWorld != null) broadcast(arenaWorld.getFormattedName());
        getPlayers().values().forEach(gp -> {
            Player p = gp.getPlayer();
            if (p != null) getKitManager().giveSelectorItem(p);
        });
    }

    @Override
    protected void onPlayerJoin(GamePlayer gp) {
        if (isRecruiting()) {
            Player p = gp.getPlayer();
            if (p != null) getKitManager().giveSelectorItem(p);
        }
    }

    // =========================================================================
    // onStart
    // =========================================================================

    @Override
    protected void onStart() {
        if (arenaWorld != null) {
            ringCenters.addAll(arenaWorld.getData("boost_ring"));
            Location sdl = arenaWorld.getDataPoint("supply_drop");
            if (sdl != null) supplyDropLocation = sdl;
        }

        List<Location> spawns = arenaWorld != null
                ? new ArrayList<>(arenaWorld.getSpawns("Players"))
                : List.of();
        Collections.shuffle(spawns);

        int idx = 0;
        for (GamePlayer gp : getPlayers().values()) {
            Player p = gp.getPlayer();
            if (p == null) continue;
            if (!spawns.isEmpty()) { p.teleport(spawns.get(idx % spawns.size())); idx++; }
            p.getInventory().clear();
            getKitManager().applyKit(p);
            p.setAllowFlight(false);
            p.setFlying(false);
        }

        fillChests();

        broadcast(ChatColor.AQUA + "Skyfall has begun! " + ChatColor.GRAY + "Glide, fight, survive!");
        broadcast(ChatColor.YELLOW + "Islands will start crumbling in " +
                (CRUMBLE_DELAY_MS / 60000) + " minutes!");
    }

    // =========================================================================
    // UpdateEvent handlers
    // =========================================================================

    /** Check timers every second — crumble, supply drop warning, supply drop spawn. */
    @EventHandler
    public void onUpdateSec(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC || !isLive()) return;
        long elapsed = System.currentTimeMillis() - getStartTime();

        if (!crumbleStarted && elapsed >= CRUMBLE_DELAY_MS) {
            crumbleStarted = true;
            broadcast(ChatColor.RED + "The islands are crumbling!");
            startCrumble();
        }
        if (!supplyDropAnnounced && elapsed >= SUPPLY_DROP_WARN_MS) {
            supplyDropAnnounced = true;
            broadcast(ChatColor.GOLD + "A supply drop will land in §e30 seconds§6!");
        }
        if (!supplyDropActive && elapsed >= SUPPLY_DROP_DELAY_MS) {
            supplyDropActive = true;
            spawnSupplyDrop();
        }
    }

    /** Check booster rings every 5 ticks (FASTER). */
    @EventHandler
    public void onUpdateFaster(UpdateEvent event) {
        if (event.getType() != UpdateType.FASTER || !isLive()) return;
        checkBoosterRings();
    }

    // =========================================================================
    // SoloGame hooks
    // =========================================================================

    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        super.onPlayerEliminated(gp); // SoloGame: checkWinCondition
        Player p = gp.getPlayer();
        String reason = (p != null && voidDeaths.remove(p.getUniqueId()))
                ? "fell into the void" : "was eliminated";
        broadcast(ChatColor.GRAY + gp.getName() + " " + reason + "! §e(" + getAliveCount() + " left)");
    }

    @Override
    protected void onWinnerDecided(GamePlayer winner) {
        if (winner == null) broadcast(ChatColor.GRAY + "No winner — everyone fell!");
        else broadcast(ChatColor.AQUA + "" + ChatColor.BOLD + winner.getName()
                + ChatColor.RESET + ChatColor.AQUA + " wins Skyfall!");
    }

    // =========================================================================
    // onEnd / onDead
    // =========================================================================

    @Override
    protected void onEnd() {
        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), this::destroy, 100L);
    }

    @Override
    protected void onDead() {
        if (arenaWorld != null) {
            MapLoader.unload(arenaWorld.getWorld()).thenRun(() ->
                    AltaraPaper.getPaperInstance().getLogger()
                            .info("[Skyfall] World unloaded for instance " + getShortId()));
            setArenaWorld(null);
        }
    }

    // =========================================================================
    // Block events
    // =========================================================================

    @EventHandler public void preventBlockBurn(BlockBurnEvent e)   { e.setCancelled(true); }
    @EventHandler public void preventLeafDecay(LeavesDecayEvent e) { e.setCancelled(true); }
    @EventHandler public void preventFade(BlockFadeEvent e)         { e.setCancelled(true); }

    // =========================================================================
    // Void kill boundary
    // =========================================================================

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isLive()) return;
        Player player = event.getPlayer();
        if (!hasPlayer(player)) return;
        GamePlayer gp = getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isAlive()) return;

        Location loc = event.getTo();
        if (loc == null) return;
        int minY = arenaWorld != null ? arenaWorld.getMin().getBlockY() : 0;
        if (loc.getY() < minY - 10) {
            voidDeaths.add(player.getUniqueId());
            eliminatePlayer(player); // base class handles kit removal + spectator + onPlayerEliminated
        }
    }

    // =========================================================================
    // Booster rings
    // =========================================================================

    private void checkBoosterRings() {
        for (GamePlayer gp : getPlayers().values()) {
            if (!gp.isAlive()) continue;
            Player p = gp.getPlayer();
            if (p == null || !p.isGliding()) continue;

            for (Location ring : ringCenters) {
                if (!ring.getWorld().equals(p.getWorld())) continue;
                if (ring.distanceSquared(p.getLocation()) > RING_DETECT_RADIUS * RING_DETECT_RADIUS) continue;

                Map<Location, Long> playerCooldowns = ringCooldowns.computeIfAbsent(
                        p.getUniqueId(), k -> new HashMap<>());
                long lastHit = playerCooldowns.getOrDefault(ring, 0L);
                if (System.currentTimeMillis() - lastHit < 2_000) continue;
                playerCooldowns.put(ring, System.currentTimeMillis());

                boostPlayer(p, RING_BOOST_STRENGTH, ring);
            }
        }
    }

    private void boostPlayer(Player player, double baseStrength, Location ring) {
        PlayerBoostRingEvent event = new PlayerBoostRingEvent(player, baseStrength);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        double strength = event.getStrength();
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(dir.multiply(strength));
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1.2f);
        player.sendMessage(ChatColor.YELLOW + "Boost ring!");
    }

    // =========================================================================
    // Chest loot
    // =========================================================================

    private void fillChests() {
        if (arenaWorld == null) return;
        for (Location loc : arenaWorld.getData("island_chest")) {
            Block block = loc.getBlock();
            if (!(block.getState() instanceof Chest chest)) continue;
            var inv = chest.getBlockInventory();
            inv.clear();
            fillIslandChest(inv);
        }
    }

    private void fillIslandChest(org.bukkit.inventory.Inventory inv) {
        Random rng = new Random();
        List<ItemStack> items = buildIslandLoot(rng);
        int count = 4 + rng.nextInt(4);
        for (int i = 0; i < count && !items.isEmpty(); i++) {
            ItemStack item = items.remove(rng.nextInt(items.size()));
            int slot = rng.nextInt(inv.getSize());
            while (inv.getItem(slot) != null) slot = rng.nextInt(inv.getSize());
            inv.setItem(slot, item);
        }
    }

    private List<ItemStack> buildIslandLoot(Random rng) {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(Material.BAKED_POTATO, 2 + rng.nextInt(3)));
        if (rng.nextBoolean()) list.add(new ItemStack(Material.COOKED_BEEF, 1 + rng.nextInt(3)));
        Material[] swords = { Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_AXE };
        list.add(new ItemStack(swords[rng.nextInt(swords.length)]));
        if (rng.nextDouble() < 0.5) list.add(new ItemStack(Material.LEATHER_HELMET));
        if (rng.nextDouble() < 0.4) list.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        if (rng.nextDouble() < 0.3) list.add(new ItemStack(Material.GOLDEN_BOOTS));
        if (rng.nextDouble() < 0.3) {
            list.add(new ItemStack(Material.BOW));
            list.add(new ItemStack(Material.ARROW, 3 + rng.nextInt(5)));
        }
        if (rng.nextDouble() < 0.2) list.add(new ItemStack(Material.GOLDEN_APPLE));
        if (rng.nextDouble() < 0.15) list.add(new ItemStack(Material.ENDER_PEARL, 1 + rng.nextInt(2)));
        return list;
    }

    // =========================================================================
    // Supply drop
    // =========================================================================

    private void spawnSupplyDrop() {
        if (supplyDropLocation == null) return;
        broadcast(ChatColor.GOLD + "A supply drop has landed!");

        Block block = supplyDropLocation.getBlock();
        if (block.getType() != Material.CHEST) block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        var inv = chest.getBlockInventory();
        inv.clear();
        inv.addItem(new ItemStack(Material.DIAMOND_HELMET));
        inv.addItem(new ItemStack(Material.DIAMOND_LEGGINGS));
        inv.addItem(new ItemStack(Material.DIAMOND_BOOTS));
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        inv.addItem(sword);
        inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        inv.addItem(new ItemStack(Material.BOW));
        inv.addItem(new ItemStack(Material.ARROW, 8));

        Bukkit.getScheduler().runTaskTimer(AltaraPaper.getPaperInstance(), task -> {
            if (!isLive()) { task.cancel(); return; }
            supplyDropLocation.getWorld().spawnParticle(Particle.END_ROD,
                    supplyDropLocation.clone().add(0, 1, 0), 3, 0.1, 1, 0.1, 0.02);
        }, 0L, 5L);
    }

    // =========================================================================
    // Map crumble
    // =========================================================================

    private void startCrumble() {
        if (arenaWorld == null) return;
        World world = arenaWorld.getWorld();
        Location min = arenaWorld.getMin();
        Location max = arenaWorld.getMax();
        Random rng = new Random();

        Bukkit.getScheduler().runTaskTimer(AltaraPaper.getPaperInstance(), task -> {
            if (!isLive()) { task.cancel(); return; }
            for (int i = 0; i < 5; i++) {
                int x = min.getBlockX() + rng.nextInt(max.getBlockX() - min.getBlockX() + 1);
                int z = min.getBlockZ() + rng.nextInt(max.getBlockZ() - min.getBlockZ() + 1);
                for (int y = max.getBlockY(); y >= min.getBlockY(); y--) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir() && block.getType() != Material.CHEST) {
                        block.setType(Material.AIR);
                        break;
                    }
                }
            }
        }, 0L, 20L);
    }
}

