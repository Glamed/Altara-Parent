package games.sparking.altara.teleport.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Broadcast when a player wants to teleport to another player somewhere
 * on the network.
 *
 * Every Paper server receives this packet. The server currently hosting
 * the target responds with a TeleportFoundPacket.
 */
@NoArgsConstructor
@AllArgsConstructor
public class TeleportRequestPacket extends Packet {

    private UUID senderUuid;
    private UUID targetUuid;
    private boolean vanish;

    @Override
    public void receive() {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) return;

        AltaraPaper.getPaperInstance().getTeleportService().prepare(
                senderUuid,
                targetUuid,
                target.getName(),
                vanish
        );

        new TeleportFoundPacket(
                senderUuid,
                targetUuid,
                target.getName(),
                vanish,
                AltaraPaper.getSharedInstance().getLocalServerName()
        ).publish();
    }
}