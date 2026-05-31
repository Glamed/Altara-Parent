package games.sparking.altara.leaderboards;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.leaderboard.LeaderboardCategory;
import games.sparking.altara.hologram.leaderboard.LeaderboardHologram;
import games.sparking.altara.hologram.listener.HologramListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows two staff-only leaderboard holograms to permitted players on join
 * and cleans them up on quit.  Register this as a Bukkit listener — it is
 * completely self-contained and has no coupling to {@link HologramListener}.
 */
public class StaffHologramManager implements Listener {

    private static final String STAFF_PERMISSION = "altara.holograms";

    private static final double LB_X   = 54.5, LB_Y   = 68, LB_Z   = 91.5;
    private static final double RISK_X = 71.5, RISK_Y = 68, RISK_Z = 90.5;

    /** Active leaderboard holograms per player UUID. */
    private final Map<UUID, List<LeaderboardHologram>> activeHolograms = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(STAFF_PERMISSION)) return;

        Plugin plugin = AltaraPaper.getPlugin();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            showFor(player);
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hideFor(event.getPlayer());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void showFor(Player player) {
        if (activeHolograms.containsKey(player.getUniqueId())) return;

        List<LeaderboardHologram> holograms = new ArrayList<>();

        // --- Staff action leaderboard (54.5, 68, 91.5) ---
        List<LeaderboardCategory> lbCategories = new ArrayList<>();
        for (String cat : FakeLeaderboardData.CATEGORIES) {
            lbCategories.add(new LeaderboardCategory(
                    FakeLeaderboardData.titleFor(cat),
                    FakeLeaderboardData.get(cat)));
        }
        Location lbLoc = new Location(player.getWorld(), LB_X, LB_Y, LB_Z);
        LeaderboardHologram lbHologram = new LeaderboardHologram(player, lbLoc, lbCategories, 5);
        lbHologram.spawn();
        lbHologram.start();
        holograms.add(lbHologram);

        // --- Player risk leaderboard (71.5, 68, 90.5) ---
        List<LeaderboardCategory> riskCategories = new ArrayList<>();
        for (String cat : FakePlayerRiskData.CATEGORIES) {
            riskCategories.add(new LeaderboardCategory(
                    FakePlayerRiskData.titleFor(cat),
                    FakePlayerRiskData.get(cat)));
        }
        Location riskLoc = new Location(player.getWorld(), RISK_X, RISK_Y, RISK_Z);
        LeaderboardHologram riskHologram = new LeaderboardHologram(player, riskLoc, riskCategories, 5);
        riskHologram.spawn();
        riskHologram.start();
        holograms.add(riskHologram);

        activeHolograms.put(player.getUniqueId(), holograms);
    }

    private void hideFor(Player player) {
        List<LeaderboardHologram> holograms = activeHolograms.remove(player.getUniqueId());
        if (holograms == null) return;
        for (LeaderboardHologram hologram : holograms) {
            hologram.stop();
        }
    }
}
