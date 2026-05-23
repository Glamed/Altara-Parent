package games.sparking.altara.queue.packet.update;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class QueueRatePacket extends Packet {

    private String queueName;
    private int rate;

    @Override
    public void receive() {
        if (Altara.getSharedInstance().getLocalServerName().equals(queueName)) {
            AltaraPaper.getPaperInstance().getLocalConfig().setQueueRate(rate);
            Altara.getSharedInstance().saveMainConfig();
        }
    }
}
