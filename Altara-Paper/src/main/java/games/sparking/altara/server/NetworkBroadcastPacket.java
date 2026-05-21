package games.sparking.altara.server;

import games.sparking.altara.redis.packet.Packet;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public class NetworkBroadcastPacket extends Packet {

    private final String message;

    @Override
    public void receive() {
        MiniMessage mm = MiniMessage.miniMessage();
        Bukkit.broadcast(mm.deserialize(message));
    }
}
