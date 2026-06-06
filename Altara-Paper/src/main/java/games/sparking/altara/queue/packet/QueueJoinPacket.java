package games.sparking.altara.queue.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class QueueJoinPacket extends Packet {

    private String queueName;
    private UUID playerUuid;

    @Override
    public void receive() {
        if (AltaraPaper.getPaperInstance().getLocalServerName().equals(queueName))
            AltaraPaper.getPaperInstance().getProfileService().loadProfile(playerUuid, profile ->
                AltaraPaper.getPaperInstance().getQueue().addPlayer(profile), true);
    }
}
