package games.sparking.altara.report;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.presence.PresenceService;
import games.sparking.altara.staffmode.StaffMode;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Puts a staff member "into" a report they now hold: staff mode (vanished, creative
 * toolkit), the report panel in chat, and a follow to wherever the suspect is on the
 * network.  Used the same way whether the report came from {@code /reporthandle} or was
 * handed to them from the website, and for every report group — a chat report still drops
 * them into staff mode next to the suspect.
 */
public final class ReportEngagement {

    private ReportEngagement() {}

    /**
     * Blocking (name + presence lookups) — call off the main thread; the in-game part hops
     * back onto it.
     *
     * @param fromWeb whether the website assigned this, so the staff member is told why
     *                they've suddenly been pulled into a report
     */
    public static void engage(UUID staffUuid, Report report, boolean fromWeb) {
        UUID suspectUuid = UUID.fromString(report.getSuspectUuid());
        String name = UUIDCache.getName(suspectUuid);
        String suspectName = name != null ? name : report.getSuspectUuid();

        // Resolve where the suspect is now, off-thread (Redis), unless they're right here.
        String suspectServer = Bukkit.getPlayer(suspectUuid) != null ? null : PresenceService.getServer(suspectUuid);

        Tasks.run(() -> {
            Player staff = Bukkit.getPlayer(staffUuid);
            if (staff == null) return;

            StaffMode.ensureSpectating(staff);

            if (fromWeb) {
                staff.sendMessage(CC.notice("Report assigned from the web panel.", "You're now handling *" + suspectName + "*."));
            }

            ReportHandleCommand.display(staff, report, suspectName);
            AltaraPaper.getPaperInstance().getReportFollowService()
                    .followSuspect(staff, suspectUuid, suspectName, suspectServer);
        });
    }
}
