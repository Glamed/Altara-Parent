package games.sparking.altara.report.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.report.ReportService;
import games.sparking.altara.report.ReportStatus;
import games.sparking.altara.utils.CC;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Published by the Web API whenever a {@code Report}'s status, handler, or handler
 * server changes (claimed, released, resolved).  Every Paper server uses this to keep its
 * local "who is handling what" cache honest — the report itself always lives in MongoDB,
 * fetched through the API — and to tell an in-game handler when the website closed or
 * released the report they were working on.
 *
 * <p>Mirrors the behaviour of the punishment packets: cheap, idempotent, safe to receive
 * more than once.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ReportUpdatedPacket extends Packet {

    private String reportId;
    private String status;
    private String handler;
    private String handlerServer;

    /** True only on the very first reason a report receives — used to ping idle staff. */
    private boolean newReport;

    /** {@link ReportService#SOURCE_GAME} when the change came from in-game, otherwise the website. */
    private String source;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER) return;

        ReportStatus parsed = ReportStatus.parse(status, null);

        // A claim (status == IN_PROGRESS) is handled entirely by the direct API response
        // the claiming server already received — only stale/closed state needs eviction.
        if (parsed != ReportStatus.IN_PROGRESS) {
            var evicted = Altara.getSharedInstance().getReportService().evictLocalHandling(reportId, handler);

            if (!ReportService.SOURCE_GAME.equals(source)) {
                for (UUID staffUuid : evicted) {
                    Player staff = Bukkit.getPlayer(staffUuid);
                    if (staff == null) continue;

                    if (parsed == ReportStatus.PENDING) {
                        staff.sendMessage(CC.notice("Your report was released.", "It's back in the queue."));
                    } else if (parsed != null) {
                        staff.sendMessage(CC.notice("Your report was closed from the web panel.",
                                "Marked as *" + parsed.name().toLowerCase() + "*."));
                    }
                }
            }
        }

        if (newReport) {
            Altara.getSharedInstance().getReportService().notifyNewReport(reportId);
        }
    }
}
