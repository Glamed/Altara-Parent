package games.sparking.altara.report.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.task.Tasks;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * Published by the Web API when a report is handed to a staff member from the website
 * ({@code POST /api/report/{id}/engage} or {@code /api/report/engage-next}).  The claim has
 * already been written to MongoDB by the time this goes out — the server the staff member
 * is on just has to act on it: put them in staff mode, show the report, and send them to
 * the suspect, exactly as if they had run {@code /reporthandle} themselves.
 *
 * <p>The in-game half lives in Altara-Paper and is plugged in through
 * {@link games.sparking.altara.report.ReportService#setEngageHandler}, since this module
 * can't depend on staff mode or the follow service.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ReportEngagePacket extends Packet {

    private String reportId;
    private String staffUuid;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER) return;

        UUID staff = UUID.fromString(staffUuid);
        if (Bukkit.getPlayer(staff) == null) return; // they're on another server

        // Fetch the fresh report off the Redis thread's back — it's a blocking API call.
        Tasks.runAsync(() -> Altara.getSharedInstance().getReportService().engageLocally(staff, reportId));
    }
}
