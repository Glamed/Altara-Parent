package games.sparking.altara.queue.packet;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.CC;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class QueueSendPlayerPacket extends Packet {

    private String queueName;
    private UUID playerUuid;

    @Override
    public void receive() {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null)
            return;

        player.sendMessage(CC.successMsg("Connecting you to " + queueName + "."));

        // Mark as confirmed switch so the quit handler won't broadcast a "staff left network" message.
        AltaraPaper.getPaperInstance().markServerSwitch(playerUuid);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(this.queueName);
        player.sendPluginMessage(AltaraPaper.getPlugin(), "BungeeCord", out.toByteArray());
    }
}
