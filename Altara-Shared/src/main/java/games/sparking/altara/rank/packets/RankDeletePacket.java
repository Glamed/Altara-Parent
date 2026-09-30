package games.sparking.altara.rank.packets;

import games.sparking.altara.Altara;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Published by the API after a rank is deleted; every server drops it locally. */
@NoArgsConstructor
@AllArgsConstructor
public class RankDeletePacket extends Packet {

    private UUID uuid;

    @Override
    public void receive() {
        Altara.getSharedInstance().getRankService().removeLocally(uuid);
    }
}
