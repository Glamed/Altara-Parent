package games.sparking.altara.game.games.bomblobbers;

import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.games.bomblobbers.event.BombPreExplodeEvent;
import games.sparking.altara.game.games.bomblobbers.event.BombThrowEvent;
import games.sparking.altara.game.games.bomblobbers.kit.*;
import games.sparking.altara.game.impl.TeamGame;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.team.GameTeam;
import games.sparking.altara.game.team.TeamColor;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * <h1>Bomb Lobbers</h1>
 *
 * A two-team game where players throw TNT at each other.
 * The last team with alive members wins.
 */
public class BombLobbers extends TeamGame {

    private static final int    MAX_TNT               = 3;
    private static final long   REFILL_INTERVAL_MS    = 2_000L;  // 2 s
    private static final int    STARTING_FUSE_TICKS   = 60;
    private static final int    RECYCLE_FUSE_THRESHOLD = 20;
    private static final long   CONTACT_PRIME_DELAY_MS = 3_000;
    private static final long   HARD_PRIME_DELAY_MS    = 8_000;
    private static final double DIRECT_HIT_RADIUS      = 0.75;
    private static final int    SLOT_TNT               = 0;

    private final Map<TNTPrimed, BombToken> _activeBombs = new ConcurrentHashMap<>();
    private final Map<GameTeam, Location>   _teamCenters = new HashMap<>();

    public BombLobbers() {
        getKitManager().registerKit(new KitJumper(this));
        getKitManager().registerKit(new KitArmorer(this));
        getKitManager().registerKit(new KitPitcher(this));
        getKitManager().registerKit(new KitWaller(this));
    }

    @Override public String getName()        { return "Bomb Lobbers"; }
    @Override public String getDescription() { return "Throw TNT at the enemy team – last team alive wins!"; }
    @Override public int    getMinPlayers()  { return 2; }
    @Override public int    getMaxPlayers()  { return 16; }
    @Override protected String getMapType()  { return "bomblobbers"; }

    // =========================================================================
    // onRecruit
    // =========================================================================

    @Override
    protected void onRecruit() {
        super.onRecruit(); // map name + kit selector
        broadcast("§6§lBomb Lobbers §r§7– Right-click the §eKit Selector §7compass to choose a kit!");
    }

    // =========================================================================
    // onStart
    // =========================================================================

    @Override
    protected void onStart() {
        GameTeam redTeam  = createTeam("red",  "Red",  TeamColor.RED);
        GameTeam blueTeam = createTeam("blue", "Blue", TeamColor.BLUE);
        distributePlayersEvenly();

        List<Location> redSpawns  = arenaWorld != null ? new ArrayList<>(arenaWorld.getSpawns("Red"))  : List.of();
        List<Location> blueSpawns = arenaWorld != null ? new ArrayList<>(arenaWorld.getSpawns("Blue")) : List.of();

        for (GameTeam team : getTeams()) {
            Location center = averageLocation(
                    team.getAllPlayers().stream()
                            .map(GamePlayer::getPlayer).filter(Objects::nonNull)
                            .map(Player::getLocation).collect(Collectors.toList()));
            if (center != null) _teamCenters.put(team, center);
        }

        for (GameTeam team : getTeams()) {
            List<Location> spawns = team.equals(redTeam) ? redSpawns : blueSpawns;
            int spawnIdx = 0;
            for (GamePlayer gp : team.getAllPlayers()) {
                Player p = gp.getPlayer();
                if (p == null) continue;
                if (!spawns.isEmpty()) { p.teleport(spawns.get(spawnIdx % spawns.size())); spawnIdx++; }
                p.getInventory().setItem(8, null);
                applyTeamArmour(p, team.getColor());
                getKitManager().applyKit(p);
                p.getInventory().setItem(SLOT_TNT, new ItemStack(Material.TNT, MAX_TNT));
                p.updateInventory();
                p.sendMessage("§6Playing as §e" + getKitManager().getKit(p).getName() + "§6!");
            }
        }

        broadcast("§6§lBomb Lobbers §r§7has begun! §cLeft/right-click §7TNT to throw it!");
    }

    // =========================================================================
    // UpdateEvent handlers
    // =========================================================================

    /** Update active bombs every tick — direct-hit detection, trail, fuse recycling. */
    @EventHandler
    public void onUpdateTick(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK || !isLive()) return;
        updateBombs();
    }

    /** Refill TNT every 2 seconds (TWOSEC). */
    @EventHandler
    public void onUpdateTwoSec(UpdateEvent event) {
        if (event.getType() != UpdateType.TWOSEC || !isLive()) return;
        refillTNT();
    }

    // =========================================================================
    // TeamGame hooks
    // =========================================================================

    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        super.onPlayerEliminated(gp); // TeamGame: fire onTeamEliminated + checkWinCondition
        GameTeam team = gp.getTeam();
        if (team != null) broadcast(team.getColor().prefix() + gp.getName() + " §7has been eliminated!");
    }

    @Override
    protected void onTeamEliminated(GameTeam team) {
        broadcast(team.getColor().prefix() + team.getName() + " Team §7has been eliminated!");
    }

    @Override
    protected void onWinnerDecided(GameTeam winner) {
        if (winner == null) broadcast("§7Draw! No team survived.");
        else broadcast(winner.getColor().prefix() + "§l" + winner.getName() + " Team §r§6wins Bomb Lobbers!");
    }

    // =========================================================================
    // onDead
    // =========================================================================

    @Override
    protected void onDead() {
        _activeBombs.keySet().forEach(Entity::remove);
        _activeBombs.clear();
        super.onDead(); // unload world
    }

    // =========================================================================
    // TNT throw
    // =========================================================================

    @EventHandler
    public void onThrow(PlayerInteractEvent event) {
        if (!isLive() || event.getAction() == Action.PHYSICAL) return;
        Player player = event.getPlayer();
        if (!hasPlayer(player)) return;
        GamePlayer gp = getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isAlive()) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() != Material.TNT) return;
        event.setCancelled(true);

        if (held.getAmount() > 1) held.setAmount(held.getAmount() - 1);
        else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        player.updateInventory();

        Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
        TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class, entity -> {
            entity.setFuseTicks(STARTING_FUSE_TICKS);
            entity.setVelocity(player.getLocation().getDirection().multiply(2.0).add(new Vector(0, 0.1, 0)));
        });

        BombToken token = new BombToken(player);
        _activeBombs.put(tnt, token);
        Bukkit.getPluginManager().callEvent(new BombThrowEvent(this, player, tnt));
    }

    // =========================================================================
    // Explosion handling
    // =========================================================================

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!isLive() || !(event.getEntity() instanceof TNTPrimed tnt)) return;
        BombToken token = _activeBombs.remove(tnt);
        if (token == null) return;

        Player thrower = Bukkit.getPlayer(token.thrower);
        if (thrower == null) { event.setCancelled(true); return; }

        GameTeam throwerTeam   = getTeamOf(thrower).orElse(null);
        GameTeam explosionSide = getSide(event.getLocation());
        if (throwerTeam != null && throwerTeam.equals(explosionSide)) { event.setCancelled(true); return; }

        BombPreExplodeEvent pre = new BombPreExplodeEvent(this, thrower, tnt, event.getLocation());
        Bukkit.getPluginManager().callEvent(pre);
        if (pre.isCancelled()) { event.setCancelled(true); return; }

        modifyExplosionBlocks(event);

        Location blast = event.getLocation();
        for (GamePlayer gp : getPlayers().values()) {
            if (!gp.isAlive()) continue;
            if (throwerTeam != null && throwerTeam.hasPlayer(gp.getPlayer())) continue;
            Player target = gp.getPlayer();
            if (target == null || target.getLocation().distanceSquared(blast) > 100) continue;
            Vector kb = target.getLocation().toVector().subtract(blast.toVector()).normalize().multiply(0.6);
            kb.setY(Math.max(kb.getY(), 0.3));
            target.setVelocity(target.getVelocity().add(kb));
        }
    }

    // =========================================================================
    // Tick helpers
    // =========================================================================

    private void refillTNT() {
        for (GamePlayer gp : getPlayers().values()) {
            if (!gp.isAlive()) continue;
            Player p = gp.getPlayer();
            if (p == null) continue;
            ItemStack slot = p.getInventory().getItem(SLOT_TNT);
            int held = (slot != null && slot.getType() == Material.TNT) ? slot.getAmount() : 0;
            if (held < MAX_TNT) { p.getInventory().setItem(SLOT_TNT, new ItemStack(Material.TNT, held + 1)); p.updateInventory(); }
        }
    }

    private void updateBombs() {
        List<TNTPrimed> remove = new ArrayList<>();
        for (Map.Entry<TNTPrimed, BombToken> entry : _activeBombs.entrySet()) {
            TNTPrimed tnt   = entry.getKey();
            BombToken token = entry.getValue();
            if (tnt == null || !tnt.isValid()) { remove.add(tnt); continue; }
            Player thrower = Bukkit.getPlayer(token.thrower);
            if (thrower == null) { tnt.remove(); remove.add(tnt); continue; }
            if (!token.directHit) checkDirectHit(tnt, token, thrower);
            drawTrail(tnt, token, thrower);
            if (!token.primed) {
                long age = token.age();
                if      (age >= HARD_PRIME_DELAY_MS)                               { token.primed = true; tnt.setFuseTicks(0); }
                else if (age >= CONTACT_PRIME_DELAY_MS && isTouchingBlock(tnt))    { token.primed = true; tnt.setFuseTicks(0); }
                else if (tnt.getFuseTicks() <= RECYCLE_FUSE_THRESHOLD)             { tnt.setFuseTicks(STARTING_FUSE_TICKS); }
            }
        }
        remove.forEach(_activeBombs::remove);
    }

    private void checkDirectHit(TNTPrimed tnt, BombToken token, Player thrower) {
        GameTeam throwerTeam = getTeamOf(thrower).orElse(null);
        for (Entity nearby : tnt.getNearbyEntities(DIRECT_HIT_RADIUS, DIRECT_HIT_RADIUS, DIRECT_HIT_RADIUS)) {
            if (!(nearby instanceof Player target)) continue;
            if (!hasPlayer(target)) continue;
            GamePlayer tgp = getGamePlayer(target).orElse(null);
            if (tgp == null || !tgp.isAlive()) continue;
            if (throwerTeam != null && throwerTeam.hasPlayer(target)) continue;
            token.directHit = true;
            target.damage(8.0, thrower);
            Vector kb = target.getLocation().toVector().subtract(tnt.getLocation().toVector()).normalize().multiply(0.5);
            kb.setY(Math.max(kb.getY(), 0.25));
            target.setVelocity(target.getVelocity().add(kb));
            thrower.sendMessage("§a⚡ Direct Hit on §e" + target.getName() + "§a!");
            token.primed = true;
            tnt.setFuseTicks(0);
            break;
        }
    }

    private void drawTrail(TNTPrimed tnt, BombToken token, Player thrower) {
        if (tnt.isOnGround()) return;
        token.prevLoc = token.currLoc;
        token.currLoc = tnt.getLocation();
        if (token.prevLoc == null || token.currLoc == null) return;
        GameTeam team = getTeamOf(thrower).orElse(null);
        Color dust = (team != null && team.getId().equals("blue")) ? Color.fromRGB(0, 191, 255) : Color.RED;
        Particle.DustOptions opts = new Particle.DustOptions(dust, 1.0f);
        double dist = token.prevLoc.distance(token.currLoc);
        Vector step = token.currLoc.toVector().subtract(token.prevLoc.toVector()).normalize().multiply(0.2);
        Location cursor = token.prevLoc.clone();
        double walked = 0;
        while (walked <= dist) {
            token.prevLoc.getWorld().spawnParticle(Particle.DUST, cursor.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0, opts);
            cursor.add(step);
            walked += 0.2;
        }
    }

    private boolean isTouchingBlock(TNTPrimed tnt) {
        Location loc = tnt.getLocation();
        int[][] offs = {{0,0,0},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int[] o : offs) {
            Block b = loc.clone().add(o[0], o[1], o[2]).getBlock();
            if (!b.getType().isAir() && b.getType() != Material.TNT) return true;
        }
        return false;
    }

    private void modifyExplosionBlocks(EntityExplodeEvent event) {
        Iterator<Block> iter = event.blockList().iterator();
        while (iter.hasNext()) {
            Block block = iter.next();
            if (block.getType() == Material.STONE)        { block.setType(Material.COBBLESTONE);        iter.remove(); continue; }
            if (block.getType() == Material.STONE_BRICKS) { block.setType(Material.CRACKED_STONE_BRICKS); iter.remove(); }
        }
    }


    // =========================================================================
    // Side detection
    // =========================================================================

    private GameTeam getSide(Location loc) {
        GameTeam nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Map.Entry<GameTeam, Location> e : _teamCenters.entrySet()) {
            if (!e.getValue().getWorld().equals(loc.getWorld())) continue;
            double d = e.getValue().distanceSquared(loc);
            if (d < nearestDist) { nearestDist = d; nearest = e.getKey(); }
        }
        return nearest;
    }

    // =========================================================================
    // Armour helpers
    // =========================================================================

    private void applyTeamArmour(Player player, TeamColor color) {
        org.bukkit.Color c = color.getArmorColor();
        player.getInventory().setHelmet(    dyedLeather(Material.LEATHER_HELMET,     c));
        player.getInventory().setChestplate(dyedLeather(Material.LEATHER_CHESTPLATE, c));
        player.getInventory().setLeggings(  dyedLeather(Material.LEATHER_LEGGINGS,   c));
        player.getInventory().setBoots(     dyedLeather(Material.LEATHER_BOOTS,      c));
    }

    private ItemStack dyedLeather(Material mat, org.bukkit.Color color) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    // =========================================================================
    // Geometry
    // =========================================================================

    private static Location averageLocation(List<Location> locs) {
        if (locs.isEmpty()) return null;
        double x = 0, y = 0, z = 0;
        World w = locs.get(0).getWorld();
        for (Location l : locs) { x += l.getX(); y += l.getY(); z += l.getZ(); }
        return new Location(w, x / locs.size(), y / locs.size(), z / locs.size());
    }
}

