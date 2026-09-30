package games.sparking.altara.teleport.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Published by the Paper server currently hosting the target of a
 * network teleport request.
 */
@NoArgsConstructor
@AllArgsConstructor
public class TeleportFoundPacket extends Packet {

    private UUID senderUuid;
    private UUID targetUuid;
    private String targetName;
    private boolean vanish;
    private String server;

    public TeleportFoundPacket(UUID senderUuid, UUID targetUuid, String targetName, boolean vanish) {
        this(
                senderUuid,
                targetUuid,
                targetName,
                vanish,
                AltaraPaper.getSharedInstance().getLocalServerName()
        );
    }

    @Override
    public void receive() {
        Player sender = Bukkit.getPlayer(senderUuid);
        if (sender == null) return;

        // TeleportService stores the pending teleport and sends the
        // player to the server that responded.
        AltaraPaper.getPaperInstance().getTeleportService().send(
                sender,
                targetUuid,
                targetName,
                server,
                vanish
        );
    }
}