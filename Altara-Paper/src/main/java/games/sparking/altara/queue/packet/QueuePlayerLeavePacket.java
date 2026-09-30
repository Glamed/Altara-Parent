package games.sparking.altara.queue.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A player left the network: every server drops them from its queue. */
@NoArgsConstructor
@AllArgsConstructor
public class QueuePlayerLeavePacket extends Packet {

    private UUID uuid;

    @Override
    public void receive() {
        AltaraPaper.getPaperInstance().getQueue().removePlayer(uuid);
    }
}
