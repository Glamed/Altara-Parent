package games.sparking.altara.profile;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles the full profile lifecycle for every connected player.
 *
 * <ul>
 *   <li>{@link AsyncPlayerPreLoginEvent} <b>LOWEST</b> — load or create the profile before
 *       higher-priority listeners (e.g. ban check) run. Kicks the player if the profile
 *       cannot be loaded.</li>
 *   <li>{@link PlayerJoinEvent} <b>NORMAL</b> — inject permissions and update live
 *       session fields (IP, name, joinTime, lastServer).</li>
 *   <li>{@link PlayerQuitEvent} <b>NORMAL</b> — uninject permissions, stop session
 *       timer, and save + evict the profile asynchronously.</li>
 * </ul>
 */
public class ProfileListener implements Listener {

    // ── Pre-login ──────────────────────────────────────────────────────────────

    /**
     * Runs on the async login thread. Loads an existing profile from the Web API or
     * creates a new one for first-time players.
     *
     * <p>Priority is {@code LOWEST} so the profile is in cache by the time the
     * {@code PunishmentListener} (HIGHEST) runs its ban check.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip   = event.getAddress().getHostAddress();

        // We're already on an async thread — run synchronously.
        Profile[] holder = {null};
        Altara.getSharedInstance().getProfileService()
              .getProfileOrCreate(uuid, name, ip, profile -> holder[0] = profile, false);

        if (holder[0] == null) {
            Component msg = CC.format(
                    "<red>Your profile could not be loaded.<newline>"
                    + "<gray>Please try again in a moment.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, msg);
        }
    }

    // ── Join ───────────────────────────────────────────────────────────────────

    /**
     * Fires on the main thread after the player has been fully admitted.
     * Injects permissions and stamps the current session data onto the profile.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) {
            // Shouldn't happen — pre-login guarantees a cached profile — but handle gracefully.
            player.kick(CC.format("<red>Your profile failed to load. Please reconnect."));
            return;
        }

        // Update session fields
        String ip = (player.getAddress() != null && player.getAddress().getAddress() != null)
                ? player.getAddress().getAddress().getHostAddress() : "N/A";
        profile.setName(player.getName());
        profile.setLastIp(ip);
        if (!profile.getKnownIps().contains(ip)) {
            profile.getKnownIps().add(ip);
        }
        profile.setJoinTime(System.currentTimeMillis());
        profile.setLastSeen(System.currentTimeMillis());
        profile.setLastServer(AltaraPaper.getSharedInstance().getLocalServerName());
        profile.getSession().startTimings();

        // Apply rank-based display name (legacy-formatted string is fine here)
        player.setDisplayName(profile.getDisplayName());

        // Inject permissions (requires main thread — we're on main thread here)
        AltaraPaper.getPaperInstance().getPermissionService().injectPlayer(player);

        // Persist the updated login data asynchronously
        Altara.getSharedInstance().getProfileService().updateProfile(uuid, null, true);
    }

    // ── Quit ───────────────────────────────────────────────────────────────────

    /**
     * Fires on the main thread. Uninjects permissions immediately, then saves and
     * evicts the profile asynchronously.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) return;

        // Uninject permissions on main thread (Bukkit requires it)
        AltaraPaper.getPaperInstance().getPermissionService().uninjectPlayer(player);

        // Stamp quit-time fields before handing off to async
        profile.setLastSeen(System.currentTimeMillis());
        profile.setJoinTime(-1L);
        profile.getSession().stopTimings();

        // Save, then evict caches — all on a worker thread
        Tasks.runAsync(() -> {
            Altara.getSharedInstance().getProfileService().updateProfile(uuid, null, false);
            Altara.getSharedInstance().getProfileService().removeProfile(uuid);
            Altara.getSharedInstance().getPunishmentService().invalidateCache(uuid);
        });
    }
}


