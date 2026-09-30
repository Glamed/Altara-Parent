package games.sparking.altara.queue.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Asks the server named {@code queueName} to add a player to its queue. */
@NoArgsConstructor
@AllArgsConstructor
public class QueueJoinPacket extends Packet {

    private String queueName;
    private UUID playerUuid;

    @Override
    public void receive() {
        AltaraPaper paper = AltaraPaper.getPaperInstance();
        if (!paper.getLocalServerName().equals(queueName)) return;

        Profile profile = paper.getProfileService().fetchProfile(playerUuid);
        int priority = profile == null ? 0 : profile.getQueuePriority(paper.getServerGroup());
        paper.getQueue().addPlayer(playerUuid, priority);
    }
}
