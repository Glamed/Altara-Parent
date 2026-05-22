package games.sparking.altara.rank.packets;

import games.sparking.altara.Altara;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class RankDeletePacket extends Packet {

    private final UUID uuid;

    @Override
    public void receive() {
        Rank rank = Altara.getSharedInstance().getRankService().getRank(uuid);
        if (rank != null) {
            Altara.getSharedInstance().handleRankDeletion(rank);
        }
        Altara.getSharedInstance().getRankService().deleteRank(uuid, () -> {
        });
    }

}
