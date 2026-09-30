package games.sparking.altara.profiler;

import games.sparking.altara.Altara;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.profiler.packet.ProfilerFlagPacket;
import games.sparking.altara.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the profiler for each login.
 *
 * <ul>
 *   <li>{@link AsyncPlayerPreLoginEvent} <b>MONITOR</b> — the profile is loaded (LOWEST)
 *       but not yet updated with this login's IP/name, which is exactly what the
 *       {@link ProfilerEngine} needs.  Loads an existing flag from Redis, or evaluates
 *       the login and persists a new flag.</li>
 *   <li>{@link PlayerJoinEvent} — caches the flag locally (so chat is shadow-muted) and,
 *       for new flags, alerts staff network-wide.</li>
 *   <li>{@link PlayerQuitEvent} — drops the local cache.  The flag itself stays in Redis
 *       until staff resolve it or it expires, so it follows the player across servers.</li>
 * </ul>
 */
public class ProfilerListener implements Listener {

    private static final String STAFF_PERMISSION = "altara.staff";

    /** Flag decided during pre-login, applied once the player has actually joined. */
    private record Pending(ProfilerRecord record, boolean isNew) {}

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();
        ProfilerService svc = Altara.getSharedInstance().getProfilerService();

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) return;

        if (isStaffRank(profile)) {
            svc.deleteFlag(uuid);
            return;
        }

        // Already flagged (e.g. switching servers) — keep the existing flag, don't re-alert.
        ProfilerRecord existing = svc.loadFlag(uuid);
        if (existing != null) {
            pending.put(uuid, new Pending(existing, false));
            return;
        }

        if (svc.isVerified(uuid) || svc.wasRecentlyChecked(uuid, ip)) return;

        ProfilerEngine.Result result = ProfilerEngine.evaluate(profile, event.getName(), ip);
        svc.markChecked(uuid, ip);
        if (!result.shouldFlag()) return;

        ProfilerRecord record = new ProfilerRecord(uuid, event.getName(), result.getScore(),
                result.getReasons(), result.getBannedAltCount(), ip, ProfilerService.FLAG_TTL);
        svc.saveFlag(record);
        pending.put(uuid, new Pending(record, true));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Pending entry = pending.remove(player.getUniqueId());
        if (entry == null) return;

        // Staff by permission (not just rank weight) are never shadow-muted.
        if (player.hasPermission(STAFF_PERMISSION)) {
            Tasks.runAsync(() -> Altara.getSharedInstance().getProfilerService().deleteFlag(player.getUniqueId()));
            return;
        }

        Altara.getSharedInstance().getProfilerService().cache(entry.record());
        if (entry.isNew()) {
            Tasks.runAsync(() -> new ProfilerFlagPacket(entry.record()).publish());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pending.remove(uuid);
        Altara.getSharedInstance().getProfilerService().uncache(uuid);
    }

    private static boolean isStaffRank(Profile profile) {
        Grant grant = profile.getRealCurrentGrant();
        Rank rank = grant != null ? grant.asRank() : null;
        return rank != null && rank.getWeight() >= Altara.getSharedInstance().getMainConfig().getStaffWeight();
    }
}
