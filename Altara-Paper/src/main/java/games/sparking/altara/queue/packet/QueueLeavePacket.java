package games.sparking.altara.queue.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Asks the server named {@code queueName} to remove a player from its queue. */
@NoArgsConstructor
@AllArgsConstructor
public class QueueLeavePacket extends Packet {

    private String queueName;
    private UUID playerUuid;

    @Override
    public void receive() {
        if (AltaraPaper.getSharedInstance().getLocalServerName().equals(queueName)) {
            AltaraPaper.getPaperInstance().getQueue().removePlayer(playerUuid);
        }
    }
}
