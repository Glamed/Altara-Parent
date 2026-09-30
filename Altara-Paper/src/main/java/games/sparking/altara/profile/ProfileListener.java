package games.sparking.altara.profile;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chat.impl.StaffChannel;
import games.sparking.altara.presence.PlayerPresence;
import games.sparking.altara.presence.PresenceService;
import games.sparking.altara.queue.packet.QueuePlayerLeavePacket;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.server.packet.NetworkBroadcastPacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
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
 *   <li>{@link AsyncPlayerPreLoginEvent} <b>LOWEST</b> - load or create the profile before
 *       higher-priority listeners (e.g. ban check) run; refuse the login if it can't load.</li>
 *   <li>{@link PlayerJoinEvent} <b>NORMAL</b> - claim network presence, inject permissions,
 *       update session fields and announce true network joins.</li>
 *   <li>{@link PlayerQuitEvent} <b>MONITOR</b> - wait briefly, then use Redis presence to
 *       determine whether the player switched servers or left the network entirely.</li>
 * </ul>
 */
public class ProfileListener implements Listener {

    private static final long SWITCH_GRACE_TICKS = 40L;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        Profile[] holder = {null};
        Altara.getSharedInstance().getProfileService()
                .getProfileOrCreate(uuid, name, ip, profile -> holder[0] = profile, false);

        if (holder[0] == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    CC.error("Unable to load your profile.", "Please try again in a moment."));
            return;
        }

        Altara.getSharedInstance().getUuidCache().update(uuid, name, false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) {
            player.kick(CC.error("Unable to load your profile.", "Please reconnect."));
            return;
        }

        /*
         * Read the old presence before claiming it.
         *
         * No presence means this is a true network join.
         * Existing presence means another Paper server previously owned them.
         */
        PlayerPresence previousPresence = PresenceService.get(uuid);
        boolean isNetworkJoin = previousPresence == null;

        String server = AltaraPaper.getSharedInstance().getLocalServerName();
        PresenceService.claim(uuid, server);

        String ip = player.getAddress() != null && player.getAddress().getAddress() != null
                ? player.getAddress().getAddress().getHostAddress() : "N/A";

        profile.setName(player.getName());
        profile.setLastIp(ip);

        if (!profile.getKnownIps().contains(ip)) {
            profile.getKnownIps().add(ip);
        }

        profile.setJoinTime(System.currentTimeMillis());
        profile.setLastSeen(System.currentTimeMillis());
        profile.setLastServer(server);

        profile.commitSession();
        profile.getSession().startTimings();

        player.displayName(CC.format(profile.getDisplayName()));

        AltaraPaper.getPaperInstance().getPermissionService().injectPlayer(player);
        Altara.getSharedInstance().getProfileService().updateProfile(uuid, null, true);

        if (isNetworkJoin && player.hasPermission(StaffChannel.PERMISSION)) {
            broadcastStaff(profile, "joined the network on");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile == null) return;

        boolean isStaff = player.hasPermission(StaffChannel.PERMISSION);

        AltaraPaper.getPaperInstance().getPermissionService().uninjectPlayer(player);

        profile.setLastSeen(System.currentTimeMillis());
        profile.getSession().stopTimings();

        UUID localSession = PresenceService.getLocalSession(uuid);
        String localServer = AltaraPaper.getSharedInstance().getLocalServerName();

        Tasks.runLaterAsync(() -> {
            /*
             * They rejoined this exact Paper server before cleanup ran.
             * The local profile now belongs to that new Bukkit session.
             */
            if (Bukkit.getPlayer(uuid) != null) return;

            PlayerPresence currentPresence = PresenceService.get(uuid);

            /*
             * If Redis contains a different session, another Paper server has
             * claimed this player. This was a server transfer.
             */
            boolean isSwitch = currentPresence != null
                    && localSession != null
                    && !localSession.equals(currentPresence.getSessionId());

            if (isSwitch) {
                PresenceService.forgetLocalSession(uuid);
            } else {
                /*
                 * Redis either still belongs to us or no longer contains the
                 * player. Treat this as a true network disconnect.
                 *
                 * release() is compare-and-delete, so it cannot delete a newer
                 * server's session even if one appears during this operation.
                 */
                PresenceService.release(uuid);

                if (isStaff) {
                    broadcastStaff(profile, "left the network from");
                }

                profile.setLastServer(null);
                profile.setJoinTime(-1L);

                new QueuePlayerLeavePacket(uuid).publish();
            }

            AltaraPaper.getPaperInstance().getQueueService().resetQueueData(uuid);

            Altara.getSharedInstance().getProfileService().updateProfile(uuid, null, false);
            Altara.getSharedInstance().getProfileService().removeProfile(uuid);
            Altara.getSharedInstance().getPunishmentService().invalidateCache(uuid);
        }, SWITCH_GRACE_TICKS);
    }

    /** {@code ┃ [Rank] Name joined the network on Lobby-1.} to staff on every server. */
    private static void broadcastStaff(Profile profile, String action) {
        Rank rank = profile.getRealCurrentGrant().asRank();

        Component name = CC.format(rank.getPrefix() + rank.getColor() + "<name>" + rank.getSuffix(),
                Placeholder.unparsed("name", profile.getName()));

        Component details = Component.text()
                .append(Component.text(action + " ", Theme.TEXT))
                .append(Component.text(AltaraPaper.getSharedInstance().getLocalServerName(), Theme.TEXT_STRONG))
                .append(Component.text(".", Theme.TEXT))
                .build();

        new NetworkBroadcastPacket(CC.notice(name, details), StaffChannel.PERMISSION, true).publish();
    }
}