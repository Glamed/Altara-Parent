package games.sparking.altara.report.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Paper-to-Paper only — never published by the Web module.  Asks whichever server
 * currently has {@code handlerUuid} online to send them to {@code targetServer}, where
 * the suspect they're handling a report against now is.  Published either right after a
 * claim (the suspect was already elsewhere) or by {@code ReportFollowListener} when the
 * suspect changes servers mid-review.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ReportFollowPacket extends Packet {

    private String handlerUuid;
    private String suspectUuid;
    private String suspectName;
    private String targetServer;

    @Override
    public void receive() {
        // No "am I the target server?" early-out: if the handler is already here, sendToServer
        // just teleports them to the suspect who has (re)joined.
        Player handler = Bukkit.getPlayer(UUID.fromString(handlerUuid));
        if (handler == null) return; // this server doesn't have the handler online

        AltaraPaper.getPaperInstance().getReportFollowService().sendToServer(
                handler, UUID.fromString(suspectUuid), suspectName, targetServer);
    }
}
