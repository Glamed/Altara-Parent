package games.sparking.altara.leaderboards;

import games.sparking.altara.Altara;
import games.sparking.altara.hologram.leaderboard.LeaderboardCategory;
import games.sparking.altara.hologram.leaderboard.LeaderboardEntry;
import games.sparking.altara.hologram.leaderboard.LeaderboardHologram;
import games.sparking.altara.hologram.listener.HologramListener;
import games.sparking.altara.profiler.ProfilerRecord;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows two staff-only leaderboard holograms to permitted players on join and cleans
 * them up on quit.  Register this as a Bukkit listener — it is completely self-contained
 * and has no coupling to {@link HologramListener}.
 *
 * <p>Both leaderboards are built from real {@code punishments} records via
 * {@code PunishmentService} — no placeholder data.  Fetching them is a network call, so
 * it always happens off the main thread; only the hologram spawn itself (a Bukkit API
 * call) runs back on the main thread.
 */
public class StaffHologramManager implements Listener {

    private static final String STAFF_PERMISSION = "altara.holograms";

    private static final double LB_X   = 54.5, LB_Y   = 68, LB_Z   = 91.5;
    private static final double RISK_X = 71.5, RISK_Y = 68, RISK_Z = 90.5;

    /** How many entries each leaderboard category shows. */
    private static final int TOP_N = 10;

    /** Active leaderboard holograms per player UUID. */
    private final Map<UUID, List<LeaderboardHologram>> activeHolograms = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(STAFF_PERMISSION)) return;

        // Fetching real leaderboard data means a REST round-trip — never do that on the
        // main thread. Compute everything off-thread, then hop back to spawn the holograms.
        Tasks.runLaterAsync(() -> {
            if (!player.isOnline()) return;

            List<LeaderboardCategory> staffCategories = buildStaffCategories();
            List<LeaderboardCategory> playerCategories = buildPlayerCategories();

            Tasks.run(() -> {
                if (player.isOnline()) showFor(player, staffCategories, playerCategories);
            });
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hideFor(event.getPlayer());
    }

    // -----------------------------------------------------------------------
    // Data (off the main thread)
    // -----------------------------------------------------------------------

    /** Top staff by actions issued in the last 30 days. */
    private static List<LeaderboardCategory> buildStaffCategories() {
        Map<UUID, Map<String, Long>> counts = Altara.getSharedInstance().getPunishmentService().getStaffActionCounts();
        List<LeaderboardCategory> categories = new ArrayList<>();
        categories.add(categoryFor("Bans Issued", counts, "SUSPENSION", "bans"));
        categories.add(categoryFor("Mutes Issued", counts, "CHAT_RESTRICTION", "mutes"));
        categories.add(categoryFor("Warnings Issued", counts, "WARN", "warns"));
        categories.add(categoryFor("Most Active (30d)", counts, "TOTAL", "actions"));
        return categories;
    }

    /** Top players by punishments received, all time, plus currently flagged accounts. */
    private static List<LeaderboardCategory> buildPlayerCategories() {
        Map<UUID, Map<String, Long>> counts = Altara.getSharedInstance().getPunishmentService().getPlayerActionCounts();
        List<LeaderboardCategory> categories = new ArrayList<>();
        categories.add(categoryFor("Most Banned", counts, "SUSPENSION", "bans"));
        categories.add(categoryFor("Most Muted", counts, "CHAT_RESTRICTION", "mutes"));
        categories.add(categoryFor("Most Warned", counts, "WARN", "warns"));
        categories.add(categoryFor("Most Punished", counts, "TOTAL", "actions"));
        categories.add(riskScoreCategory());
        return categories;
    }

    /**
     * Currently flagged accounts ranked by suspicion score — the real anti-cheat profiler
     * (see {@link ProfilerService}, same data as {@code /profiler}), not placeholder data.
     */
    private static LeaderboardCategory riskScoreCategory() {
        List<ProfilerRecord> flags = new ArrayList<>(Altara.getSharedInstance().getProfilerService().getAllFlags());
        flags.sort(Comparator.comparingInt(ProfilerRecord::getScore).reversed());

        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (ProfilerRecord record : flags.stream().limit(TOP_N).toList()) {
            entries.add(new LeaderboardEntry(rank++, record.getName(), record.getScore(), "pts"));
        }
        return new LeaderboardCategory("Risk Score", entries);
    }

    /** Ranks {@code counts} by one action-type key (or {@code "TOTAL"}) into a leaderboard category. */
    private static LeaderboardCategory categoryFor(String title, Map<UUID, Map<String, Long>> counts,
                                                    String key, String unit) {
        List<Map.Entry<UUID, Long>> top = counts.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().getOrDefault(key, 0L)))
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(TOP_N)
                .toList();

        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : top) {
            String name = UUIDCache.getName(entry.getKey());
            entries.add(new LeaderboardEntry(rank++, name != null ? name : "Unknown", entry.getValue(), unit));
        }
        return new LeaderboardCategory(title, entries);
    }

    // -----------------------------------------------------------------------
    // Main-thread helpers
    // -----------------------------------------------------------------------

    private void showFor(Player player, List<LeaderboardCategory> staffCategories,
                         List<LeaderboardCategory> playerCategories) {
        if (activeHolograms.containsKey(player.getUniqueId())) return;

        List<LeaderboardHologram> holograms = new ArrayList<>();

        // --- Staff action leaderboard (54.5, 68, 91.5) ---
        Location lbLoc = new Location(player.getWorld(), LB_X, LB_Y, LB_Z);
        LeaderboardHologram lbHologram = new LeaderboardHologram.Builder(player, lbLoc, staffCategories, 5)
                .clickSound(Sound.UI_BUTTON_CLICK)
                .clickSoundPitch(1.2f)
                .autoRotateBoth(200L)
                .autoRotateSound(Sound.BLOCK_NOTE_BLOCK_PLING)
                .autoRotateSoundPitch(1.5f)
                .manualInteractionHold(600L)
                .build();
        lbHologram.spawn();
        lbHologram.start();
        holograms.add(lbHologram);

        // --- Most-punished-players leaderboard (71.5, 68, 90.5) ---
        Location riskLoc = new Location(player.getWorld(), RISK_X, RISK_Y, RISK_Z);
        LeaderboardHologram riskHologram = new LeaderboardHologram.Builder(player, riskLoc, playerCategories, 5)
                .clickSound(Sound.UI_BUTTON_CLICK)
                .clickSoundPitch(1.2f)
                .autoRotateBoth(200L)
                .autoRotateSound(Sound.BLOCK_NOTE_BLOCK_PLING)
                .autoRotateSoundPitch(1.5f)
                .manualInteractionHold(600L)
                .build();
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
