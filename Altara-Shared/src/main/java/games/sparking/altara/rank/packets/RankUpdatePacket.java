package games.sparking.altara.rank.packets;

import games.sparking.altara.Altara;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class RankUpdatePacket extends Packet {

    private UUID uuid;

    @Override
    public void receive() {
        Altara.getSharedInstance().getRankService().updateRank(uuid, () -> {
        });
    }

}
