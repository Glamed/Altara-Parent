package games.sparking.altara.game.games.micro;

import games.sparking.altara.game.impl.TeamGame;
import games.sparking.altara.game.module.MapCrumbleModule;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.games.micro.kit.KitArcher;
import games.sparking.altara.game.games.micro.kit.KitFighter;
import games.sparking.altara.game.games.micro.kit.KitWorker;
import games.sparking.altara.game.team.GameTeam;
import games.sparking.altara.game.team.TeamColor;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * <h1>Micro Battle</h1>
 *
 * Teams start separated by a glass barrier that vanishes after {@value BARRIER_SECONDS}s.
 * Gather resources, fight! Last team standing wins. Map crumbles after 3 minutes.
 *
 * <h2>WorldConfig.dat</h2>
 * <ul>
 *   <li>{@code TEAM_NAME:Red / Blue / Green / Yellow} — team spawn points</li>
 *   <li>{@code CUSTOM_NAME:barrier} — barrier glass block positions</li>
 * </ul>
 */
public class MicroGame extends TeamGame {

    private static final int    BARRIER_SECONDS      = 10;
    private static final int    CHEAT_KILL_SECONDS   = 4;
    private static final double ARROW_KNOCKBACK      = 1.6;
    private static final int    MIN_FOOD             = 2;

    private final Set<Block>             barrierBlocks = new HashSet<>();
    private final Map<GameTeam, Location> teamCenters  = new HashMap<>();

    private boolean barrierRemoved = false;

    public MicroGame() {
        getKitManager().registerKit(new KitArcher(this));
        getKitManager().registerKit(new KitFighter(this));
        getKitManager().registerKit(new KitWorker(this));
    }

    @Override public String getName()        { return "Micro Battle"; }
    @Override public String getDescription() { return "Gather blocks, fight! Last team standing wins."; }
    @Override public int    getMinPlayers()  { return 2; }
    @Override public int    getMaxPlayers()  { return 16; }
    @Override protected String getMapType()  { return "micro"; }

    // =========================================================================
    // onRecruit
    // =========================================================================

    @Override
    protected void onRecruit() {
        super.onRecruit(); // map name + kit selector
        broadcast(ChatColor.GOLD + "Micro Battle — choose a kit!");
    }

    // =========================================================================
    // onStart
    // =========================================================================

    @Override
    protected void onStart() {
        // Create up to 4 teams based on player count
        List<String>   names  = List.of("Red", "Blue", "Green", "Yellow");
        TeamColor[]    colors = { TeamColor.RED, TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW };
        int teamsNeeded = Math.max(2, Math.min(getPlayers().size(), 4));
        for (int i = 0; i < teamsNeeded; i++) createTeam(names.get(i).toLowerCase(), names.get(i), colors[i]);
        distributePlayersEvenly();

        // Teleport each team and record center positions for cheater detection
        for (GameTeam team : getTeams()) {
            List<Location> spawns = arenaWorld != null
                    ? new ArrayList<>(arenaWorld.getSpawns(team.getName())) : List.of();
            int idx = 0;
            for (GamePlayer gp : team.getAllPlayers()) {
                Player p = gp.getPlayer();
                if (p == null) continue;
                p.getInventory().clear();
                if (!spawns.isEmpty()) p.teleport(spawns.get(idx++ % spawns.size()));
                getKitManager().applyKit(p);
            }
            if (!spawns.isEmpty()) teamCenters.put(team, avgLocation(spawns));
        }

        buildBarrier();
        addModule(new MapCrumbleModule(3 * 60_000L));
        broadcast(ChatColor.YELLOW + "The barrier will break in " + ChatColor.RED + BARRIER_SECONDS + "s" + ChatColor.YELLOW + "!");
    }

    // =========================================================================
    // UpdateEvent handlers
    // =========================================================================

    /** Maintain food level every tick (like Mineplex Micro). */
    @EventHandler
    public void onUpdateTick(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK || !isLive()) return;
        maintainFood();
    }

    /** Handle barrier cheater detection and removal every 5 ticks (like Mineplex Micro FASTER). */
    @EventHandler
    public void onUpdateFaster(UpdateEvent event) {
        if (event.getType() != UpdateType.FASTER || !isLive()) return;

        long elapsed = System.currentTimeMillis() - getStartTime();
        if (!barrierRemoved && elapsed < CHEAT_KILL_SECONDS * 1000L) detectCheaters();
        if (!barrierRemoved && elapsed >= BARRIER_SECONDS * 1000L) removeBarrier();
    }

    // =========================================================================
    // TeamGame hooks
    // =========================================================================

    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        super.onPlayerEliminated(gp); // TeamGame: fire onTeamEliminated + checkWinCondition
        GameTeam team = gp.getTeam();
        if (team != null) broadcast(team.getFormattedName() + ChatColor.GRAY + " " + gp.getName() + " was eliminated!");
    }

    @Override
    protected void onTeamEliminated(GameTeam team) {
        broadcast(team.getFormattedName() + ChatColor.GRAY + " has been eliminated!");
    }

    @Override
    protected void onWinnerDecided(GameTeam winner) {
        if (winner == null) broadcast(ChatColor.GRAY + "It's a draw!");
        else broadcast(winner.getFormattedName() + ChatColor.GOLD + " wins Micro Battle!");
    }

    // =========================================================================
    // onDead
    // =========================================================================

    @Override
    protected void onDead() {
        // Restore barrier glass
        barrierBlocks.forEach(b -> {
            if (b.getType() == Material.WHITE_STAINED_GLASS) b.setType(Material.AIR);
        });
        barrierBlocks.clear();
        super.onDead(); // unload world
    }

    // =========================================================================
    // Arrow knockback boost
    // =========================================================================

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!isLive()) return;
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;
        if (!hasPlayer(shooter) || !(event.getHitEntity() instanceof Player target)) return;
        if (!hasPlayer(target)) return;
        GamePlayer gp = getGamePlayer(target).orElse(null);
        if (gp == null || !gp.isAlive()) return;

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                games.sparking.altara.AltaraPaper.getPaperInstance(), () -> {
                    Vector dir = target.getLocation().toVector()
                            .subtract(shooter.getLocation().toVector()).normalize();
                    dir.setY(0.4);
                    target.setVelocity(target.getVelocity().add(dir.multiply(ARROW_KNOCKBACK - 1.0)));
                }, 1L);
    }

    // =========================================================================
    // Barrier helpers
    // =========================================================================

    private void buildBarrier() {
        if (arenaWorld == null) return;
        arenaWorld.getCustom("barrier").forEach(loc -> {
            Block b = loc.getBlock();
            b.setType(Material.WHITE_STAINED_GLASS);
            barrierBlocks.add(b);
        });
    }

    private void removeBarrier() {
        barrierRemoved = true;
        barrierBlocks.forEach(b -> b.setType(Material.AIR));
        barrierBlocks.clear();
        broadcast(ChatColor.RED + "The barrier has broken! " + ChatColor.YELLOW + "Fight!");
    }

    private void detectCheaters() {
        for (GameTeam team : getTeams()) {
            Location expected = teamCenters.get(team);
            if (expected == null) continue;
            for (GamePlayer gp : team.getAllPlayers()) {
                if (!gp.isAlive()) continue;
                Player p = gp.getPlayer();
                if (p == null) continue;

                GameTeam closest = null;
                double closestDist = Double.MAX_VALUE;
                for (var entry : teamCenters.entrySet()) {
                    if (entry.getValue().getWorld() == null || !entry.getValue().getWorld().equals(p.getWorld())) continue;
                    double d = entry.getValue().distanceSquared(p.getLocation());
                    if (d < closestDist) { closestDist = d; closest = entry.getKey(); }
                }
                if (closest != null && !closest.equals(team)) {
                    p.setHealth(0);
                    p.sendMessage(ChatColor.RED + "You cannot cross the barrier!");
                }
            }
        }
    }

    private void maintainFood() {
        for (GamePlayer gp : getPlayers().values()) {
            if (!gp.isAlive()) continue;
            Player p = gp.getPlayer();
            if (p != null && p.getFoodLevel() < MIN_FOOD) p.setFoodLevel(MIN_FOOD);
        }
    }

    // =========================================================================
    // Geometry
    // =========================================================================

    private static Location avgLocation(List<Location> locs) {
        double x = 0, y = 0, z = 0;
        World w = locs.get(0).getWorld();
        for (Location l : locs) { x += l.getX(); y += l.getY(); z += l.getZ(); }
        return new Location(w, x / locs.size(), y / locs.size(), z / locs.size());
    }
}
