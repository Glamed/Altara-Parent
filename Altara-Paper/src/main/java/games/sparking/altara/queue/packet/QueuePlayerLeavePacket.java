package games.sparking.altara.queue.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@RequiredArgsConstructor
public class QueuePlayerLeavePacket extends Packet {

    private final UUID uuid;

    @Override
    public void receive() {
        if (AltaraPaper.getPaperInstance().getQueue().getPlayers().contains(uuid))
            AltaraPaper.getPaperInstance().getProfileService().loadProfile(uuid, profile -> AltaraPaper.getPaperInstance().getQueue().removePlayer(profile), true);
    }
}
