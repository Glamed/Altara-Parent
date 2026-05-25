package games.sparking.altara.profiler;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.Altara;
import games.sparking.altara.chat.ChatService;
import games.sparking.altara.chat.impl.ShadowMuteChannel;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.profiler.packet.ProfilerFlagPacket;
import games.sparking.altara.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles the Profiler system's join-time evaluation and channel management.
 *
 * <ul>
 *   <li>{@link PlayerJoinEvent} — runs the {@link ProfilerEngine} asynchronously.
 *       If the score meets the threshold the player is flagged and silently moved
 *       into the {@link ShadowMuteChannel}.  The channel's own {@code getFormat}
 *       logic then delivers the message to the sender and to staff only.</li>
 *   <li>{@link PlayerQuitEvent} — evicts the profiler record so it doesn't
 *       accumulate stale entries.</li>
 * </ul>
 *
 * <p>Shadow-mute enforcement is entirely handled by {@link ShadowMuteChannel} —
 * there is no need for a separate {@code AsyncPlayerChatEvent} listener.
 */
public class ProfilerListener implements Listener {

    // ── Join: run the profiler engine ──────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        ProfilerService svc = Altara.getSharedInstance().getProfilerService();
        ProfilerRecord existing = svc.getRecord(uuid);

        if (existing != null) {
            if (existing.isVerified()) return; // already cleared by staff, nothing to do

            // Player was already flagged (e.g. switching servers or flag packet arrived
            // before the join event). Re-apply the shadow mute channel immediately.
            Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(),
                    () -> applyShadowMuteChannel(player));
            return;
        }

        // Run scoring logic off the main thread.
        Tasks.runAsync(() -> evaluatePlayer(player));
    }

    private void evaluatePlayer(Player player) {
        if (!player.isOnline()) return;

        UUID    uuid    = player.getUniqueId();
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) return;

        int score = ProfilerEngine.computeScore(profile);
        if (!ProfilerEngine.shouldFlag(score)) return;

        // Count alt accounts that are currently banned (compromised alt heuristic).
        int compromisedAltCount = countCompromisedAlts(profile);

        // Flag locally and broadcast to the network.
        Altara.getSharedInstance().getProfilerService().flag(uuid, player.getName(), score, compromisedAltCount);
        new ProfilerFlagPacket(uuid.toString(), player.getName(), score, compromisedAltCount).publish();

        // Switch the player into the shadow-mute channel on the main thread.
        Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(),
                () -> applyShadowMuteChannel(player));
    }

    /**
     * Silently moves a player into the {@link ShadowMuteChannel}.
     * Must be called on the main thread.
     */
    public static void applyShadowMuteChannel(Player player) {
        ChatService.setChatChannel(player, ShadowMuteChannel.getInstance(), true /* silent */);
    }

    /**
     * Restores the player's channel to whatever is saved in Redis (their previous
     * channel before the shadow mute was applied).
     * Must be called on the main thread.
     */
    public static void clearShadowMuteChannel(Player player) {
        ChatService.loadChatChannel(player.getUniqueId());
    }

    /**
     * Counts how many alt accounts (shared-IP accounts) for this profile have ever
     * been banned.  This is the "compromised alt count" shown on hover.
     */
    private int countCompromisedAlts(Profile profile) {
        int[] count = {0};
        Altara.getSharedInstance().getProfileService().getAlts(profile, alts -> {
            if (alts == null) return;
            for (Profile alt : alts) {
                boolean banned = alt.getPunishments().stream()
                        .anyMatch(p -> !p.isRemoved() && p.isBan());
                if (banned) count[0]++;
            }
        }, false);
        return count[0];
    }

    // ── Quit: clean up the record ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Altara.getSharedInstance().getProfilerService().remove(event.getPlayer().getUniqueId());
        // Channel is cleaned up automatically by ChatService.removePlayer in ChatListener.
    }
}

