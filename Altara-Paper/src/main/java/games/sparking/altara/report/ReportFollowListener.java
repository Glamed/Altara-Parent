package games.sparking.altara.report;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.report.packet.ReportFollowPacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.UUID;

/**
 * Finishes a cross-server follow once the handler lands on the destination server
 * ({@code PendingFollow}), and detects the reverse case — a suspect who is actively being
 * handled logging in somewhere new — to trigger the same follow for their handler.
 */
public class ReportFollowListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();
        ReportFollowService.PendingFollow pending = AltaraPaper.getPaperInstance()
                .getReportFollowService().get(player.getUniqueId());
        if (pending == null) return;

        Player suspect = player.getServer().getPlayer(pending.getSuspectUuid());
        if (suspect != null) event.setSpawnLocation(suspect.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Finish our own pending follow, if we have one.
        ReportFollowService.PendingFollow pending = AltaraPaper.getPaperInstance()
                .getReportFollowService().consume(player.getUniqueId());
        if (pending != null) {
            Player suspect = Bukkit.getPlayer(pending.getSuspectUuid());
            if (suspect != null) {
                AltaraPaper.getPaperInstance().getReportFollowService().teleportAndSpectate(player, suspect);
            }
        }

        // Is the player who just joined actively being handled from another server?
        Tasks.runAsync(() -> checkActiveSuspect(player.getUniqueId()));
    }

    private void checkActiveSuspect(UUID suspectUuid) {
        Altara.getSharedInstance().getReportService().getActiveForSuspect(suspectUuid).ifPresent(report -> {
            if (report.getStatus() != ReportStatus.IN_PROGRESS || report.getHandler() == null) return;

            // Always broadcast rather than trusting handlerServer: the handler may have moved
            // since claiming (e.g. following this suspect earlier).  Whichever server has the
            // handler acts on it — a cross-server hop, or a plain teleport if it's this one.
            String here = Altara.getSharedInstance().getLocalServerName();
            new ReportFollowPacket(report.getHandler(), suspectUuid.toString(),
                    UUIDCache.getName(suspectUuid), here).publish();
        });
    }

    /**
     * Deliberately does <em>not</em> release the report: following a suspect to another
     * server fires a quit here, and a website-assigned handler may not be in-game at all.
     * Abandoned claims are released by the Web API's sweep once the handler has been off
     * the network and quiet for a few minutes.  This only drops the stale local cache.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Altara.getSharedInstance().getReportService().forgetLocal(event.getPlayer().getUniqueId());
    }
}
