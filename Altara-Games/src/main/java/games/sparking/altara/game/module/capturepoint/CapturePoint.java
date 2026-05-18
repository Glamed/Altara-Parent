package games.sparking.altara.game.module.capturepoint;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.team.GameTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single beacon/point that teams contest to gain ownership of.
 *
 * <p>Wool blocks within {@value #MAX_RADIUS} blocks of the centre are used as the
 * capture-progress display: they are recoloured as a team captures the point.
 *
 * <p>Created through {@link CapturePointModule#addCapturePoint}.
 */
public class CapturePoint {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    static final int MAX_RADIUS     = 5;
    private static final int MAX_PROGRESS         = 5;
    private static final int MAX_PROGRESS_NEUTRAL  = 10;
    private static final long MIN_INFORM_INTERVAL = 30_000L; // ms

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Game      game;
    private final String    name;
    private final ChatColor colour;
    private final Location  center;

    private final List<Block> wool;
    private final List<Block> claimed = new ArrayList<>();

    private final double captureDist; // squared

    private GameTeam owner;
    private GameTeam currentSide;
    private int      progress;
    private long     lastInformTime;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    CapturePoint(Game game, String name, ChatColor colour, Location center) {
        this.game   = game;
        this.name   = name;
        this.colour = colour;
        this.center = center.clone();
        this.wool   = new ArrayList<>();

        // Collect all wool blocks within radius
        Block origin = center.getBlock();
        int r = MAX_RADIUS;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block b = origin.getRelative(x, y, z);
                    if (!isWool(b.getType())) continue;
                    double distSq = center.distanceSquared(b.getLocation().add(0.5, 0.5, 0.5));
                    if (distSq <= r * r) {
                        wool.add(b);
                    }
                }
            }
        }
        Collections.shuffle(wool);
        this.captureDist = Math.pow(MAX_RADIUS + 0.5, 2);
    }

    // -------------------------------------------------------------------------
    // Tick (called by CapturePointModule every second)
    // -------------------------------------------------------------------------

    public void update() {
        Map<GameTeam, Integer> onPoint = countPlayersOnPoint();

        GameTeam highest    = null;
        int      highCount  = 0;
        boolean  contested  = false;

        for (Map.Entry<GameTeam, Integer> e : onPoint.entrySet()) {
            if (e.getValue() > 0) {
                if (highest == null) {
                    highest   = e.getKey();
                    highCount = e.getValue();
                } else {
                    contested = true;
                    break;
                }
            }
        }

        // Contested — do nothing
        if (contested) return;

        // No one on point — slowly retreat toward neutral if there is an owner
        if (highest == null) {
            if (owner == null) return;
            highest   = owner;
            highCount = 1;
        } else {
            // Inform if a non-owner team starts capturing
            if ((owner == null || !owner.equals(highest))
                    && System.currentTimeMillis() - lastInformTime >= MIN_INFORM_INTERVAL) {
                lastInformTime = System.currentTimeMillis();
                String msg = "§8[§6Game§8] §7Team " + highest.getFormattedName()
                        + " §7is capturing the " + colour + name + " §7Beacon!";
                sendMessage(highest, msg);
                if (owner != null) sendMessage(owner, msg);
            }
        }

        // Already at max progress for current owner
        if (owner != null && owner.equals(highest) && progress >= MAX_PROGRESS) return;

        advance(highest, highCount);
    }

    // -------------------------------------------------------------------------
    // Internal capture logic
    // -------------------------------------------------------------------------

    private void advance(GameTeam team, int amount) {
        if (currentSide == null) currentSide = team;

        if (currentSide.equals(team)) {
            // Same side — increase progress
            progress += amount;
            paintWool(team, amount, true);

            int cap = (owner == null) ? MAX_PROGRESS_NEUTRAL : MAX_PROGRESS;
            if (progress >= cap) {
                progress = MAX_PROGRESS;
                capture(team);
            }
        } else {
            // Opposite side — decrease progress
            if (progress <= 0) {
                resetWool();
                currentSide = team;
                progress    = 0;
                advance(team, amount);
                return;
            }
            progress -= amount;
            paintWool(team, amount, false);
        }
    }

    private void capture(GameTeam team) {
        if (owner != null && owner.equals(team)) return;

        String msg = "§8[§6Game§8] §7Team " + team.getFormattedName()
                + " §7captured the " + colour + name + " §7Beacon!";
        if (owner != null) sendMessage(owner, msg);
        sendMessage(team, msg);

        owner = team;
        progress = MAX_PROGRESS;

        playFirework(team);
        Bukkit.getPluginManager().callEvent(new CapturePointCaptureEvent(this));
    }

    // -------------------------------------------------------------------------
    // Wool display
    // -------------------------------------------------------------------------

    private void paintWool(GameTeam team, int amount, boolean forward) {
        int cap     = (owner == null) ? MAX_PROGRESS_NEUTRAL : MAX_PROGRESS;
        int toChange = (int) Math.ceil((double) wool.size() / cap) * amount + 1;
        int changed  = 0;

        Material teamWool = teamWoolMaterial(team);

        for (Block block : wool) {
            if (changed >= toChange) break;

            if (forward) {
                if (claimed.contains(block)) continue;
                block.setType(teamWool);
                changed++;
                claimed.add(block);
            } else {
                if (!claimed.contains(block)) continue;
                block.setType(Material.WHITE_WOOL);
                changed++;
                claimed.remove(block);
            }
        }
    }

    private void resetWool() {
        for (Block b : claimed) b.setType(Material.WHITE_WOOL);
        claimed.clear();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<GameTeam, Integer> countPlayersOnPoint() {
        Map<GameTeam, Integer> map = new HashMap<>();
        for (GameTeam team : game.getTeams()) {
            int count = 0;
            for (GamePlayer gp : team.getAlivePlayers()) {
                Player p = gp.getPlayer();
                if (p == null) continue;
                // Skip spectators
                if (gp.isSpectating()) continue;
                if (isOnPoint(p.getLocation())) count++;
            }
            map.put(team, count);
        }
        return map;
    }

    private void sendMessage(GameTeam team, String message) {
        for (GamePlayer gp : team.getAllPlayers()) {
            Player p = gp.getPlayer();
            if (p == null) continue;
            p.playSound(p.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1f, 1f);
            p.sendMessage(message);
        }
    }

    private void playFirework(GameTeam team) {
        Color color = team.getColor().getArmorColor();
        Firework fw = center.getWorld().spawn(center, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().with(Type.BURST).withColor(color).build());
        fw.setFireworkMeta(meta);
        fw.detonate();
    }

    private static boolean isWool(Material m) {
        return m.name().endsWith("_WOOL");
    }

    private static Material teamWoolMaterial(GameTeam team) {
        String dyeName = team.getColor().getDyeColor().name();
        String matName = dyeName + "_WOOL";
        try {
            return Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            return Material.WHITE_WOOL;
        }
    }

    // -------------------------------------------------------------------------
    // Public accessors
    // -------------------------------------------------------------------------

    /** Returns {@code true} if {@code location} is within the capture radius. */
    public boolean isOnPoint(Location location) {
        return center.distanceSquared(location) < captureDist;
    }

    public String    getName()    { return name;   }
    public ChatColor getColour()  { return colour; }
    public GameTeam  getOwner()   { return owner;  }
    public Location  getCenter()  { return center.clone(); }

    /**
     * Returns the display string used in scoreboards:
     * the colour prefix of the owning team, or white if neutral.
     */
    public String getDisplayString() {
        return (owner != null ? owner.getColor().prefix() : "§f") + name;
    }

    // -------------------------------------------------------------------------
    // Block on/below centre support (beacon glass reskin skipped in modern API)
    // -------------------------------------------------------------------------

    /** Returns the block directly over the centre (useful for detecting beacons). */
    public Block getCentreBlock() {
        return center.getBlock();
    }

    public Block getCentreBlockAbove() {
        return center.getBlock().getRelative(BlockFace.UP);
    }
}

