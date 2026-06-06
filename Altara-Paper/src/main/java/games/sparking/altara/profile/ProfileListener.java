package games.sparking.altara.profile;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.profile.packet.ProfileServerSwitchPacket;
import games.sparking.altara.queue.packet.QueuePlayerLeavePacket;
import games.sparking.altara.server.packet.NetworkBroadcastPacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles the full profile lifecycle for every connected player.
 *
 * <ul>
 *   <li>{@link AsyncPlayerPreLoginEvent} <b>LOWEST</b> — load or create the profile before
 *       higher-priority listeners (e.g. ban check) run. Kicks the player if the profile
 *       cannot be loaded.</li>
 *   <li>{@link PlayerJoinEvent} <b>NORMAL</b> — inject permissions, update live session
 *       fields (IP, name, joinTime, lastServer), and broadcast a staff-join message when
 *       the player arrives from outside the network.</li>
 *   <li>{@link PlayerQuitEvent} / {@link PlayerKickEvent} <b>MONITOR</b> — uninject
 *       permissions, stop the session timer, broadcast a staff-quit message (unless the
 *       player is only switching servers), and save + evict the profile asynchronously.</li>
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
     * Injects permissions, stamps the current session data onto the profile, and
     * broadcasts a staff-join notice when the player joins from outside the network
     * (i.e. {@code lastServer} was {@code null} — they weren't on another server).
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

        // Broadcast a staff join message only when the player arrives from outside the network
        // (lastServer == null means they weren't on another server — true network join).
        boolean isNetworkJoin = profile.getLastServer() == null;

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

        // Broadcast staff join to staff members across the network
        if (isNetworkJoin && player.hasPermission("altara.staff")) {
            String serverName = AltaraPaper.getSharedInstance().getLocalServerName();
            Component msg = Component.text()
                    .append(CC.format(profile.getRealCurrentGrant().asRank().getPrefix()))
                    .append(CC.format(profile.getRealCurrentGrant().asRank().getColor() + profile.getName()))
                    .append(CC.format(profile.getRealCurrentGrant().asRank().getSuffix()))
                    .append(Component.text(" joined the network on ", NamedTextColor.GRAY))
                    .append(Component.text(serverName, NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.GRAY))
                    .build();
            new NetworkBroadcastPacket(msg, "altara.staff", true).publish();
        }
    }

    // ── Quit / Kick ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        handlePlayerQuit(event.getPlayer());
    }

    /**
     * Fires on the main thread. Uninjects permissions immediately, then — after a short
     * delay to allow the player to appear on their new server — broadcasts a staff-quit
     * message and saves/evicts the profile.
     *
     * <p>If the player's UUID is in {@link AltaraPaper#getConfirmedSwitch()}, we know they
     * are only switching servers (not leaving the network), so we skip the "staff left"
     * broadcast, keep {@code lastServer} intact, and do not publish a
     * {@link QueuePlayerLeavePacket}.
     */
    private void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) return;

        // Uninject permissions on main thread (Bukkit requires it)
        AltaraPaper.getPaperInstance().getPermissionService().uninjectPlayer(player);

        // Tell the rest of the network this player left this server.
        new ProfileServerSwitchPacket(uuid, AltaraPaper.getSharedInstance().getLocalServerName()).publish();

        // Stamp quit-time fields before handing off to async
        profile.setLastSeen(System.currentTimeMillis());
        profile.getSession().stopTimings();

        // Cache the staff permission synchronously (player object becomes invalid async).
        boolean isStaff = player.hasPermission("altara.staff");

        // Wait a short period so that if the player re-joins another server immediately,
        // their UUID will be added to confirmedSwitch before we check it.
        Tasks.runLaterAsync(() -> {
            boolean isSwitch = AltaraPaper.getPaperInstance().removeServerSwitch(uuid);

            if (!isSwitch) {
                // True network disconnect — broadcast staff quit and clean up queues.
                if (isStaff) {
                    String serverName = AltaraPaper.getSharedInstance().getLocalServerName();
                    Component msg = Component.text()
                            .append(CC.format(profile.getRealCurrentGrant().asRank().getPrefix()))
                            .append(CC.format(profile.getRealCurrentGrant().asRank().getColor() + profile.getName()))
                            .append(CC.format(profile.getRealCurrentGrant().asRank().getSuffix()))
                            .append(Component.text(" left the network from ", NamedTextColor.GRAY))
                            .append(Component.text(serverName, NamedTextColor.YELLOW))
                            .append(Component.text(".", NamedTextColor.GRAY))
                            .build();
                    new NetworkBroadcastPacket(msg, "altara.staff", true).publish();
                }

                // Clear server-session fields so the next join looks like a fresh network join.
                profile.setLastServer(null);
                profile.setJoinTime(-1L);

                // Notify the queue service across the network.
                new QueuePlayerLeavePacket(uuid).publish();
            }

            // Reset local queue data regardless of switch or full disconnect.
            AltaraPaper.getPaperInstance().getQueueService().resetQueueData(uuid);

            // Save, then evict caches — all on a worker thread
            Altara.getSharedInstance().getProfileService().updateProfile(uuid, null, false);
            Altara.getSharedInstance().getProfileService().removeProfile(uuid);
            Altara.getSharedInstance().getPunishmentService().invalidateCache(uuid);
        }, 40L);
    }
}


