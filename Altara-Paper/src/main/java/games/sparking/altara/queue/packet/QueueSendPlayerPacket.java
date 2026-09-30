package games.sparking.altara.queue.packet;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Sends a player (wherever they are) to {@code queueName} through the proxy. */
@NoArgsConstructor
@AllArgsConstructor
public class QueueSendPlayerPacket extends Packet {

    private String queueName;
    private UUID playerUuid;

    @Override
    public void receive() {
        Tasks.run(() -> {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) return;

            player.sendMessage(CC.info("Sending you to *" + queueName + "*..."));

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(queueName);

            player.sendPluginMessage(AltaraPaper.getPlugin(), "BungeeCord", out.toByteArray());
        });
    }
}