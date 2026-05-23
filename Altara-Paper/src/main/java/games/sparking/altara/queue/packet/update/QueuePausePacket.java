package games.sparking.altara.queue.packet.update;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class QueuePausePacket extends Packet {

    private String queueName;
    private boolean paused;

    @Override
    public void receive() {
        if (Altara.getSharedInstance().getLocalServerName().equals(queueName)) {
            AltaraPaper.getPaperInstance().getLocalConfig().setQueuePaused(paused);
            AltaraPaper.getPaperInstance().saveMainConfig();
        }
    }
}
