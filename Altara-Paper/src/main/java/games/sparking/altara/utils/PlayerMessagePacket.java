package games.sparking.altara.utils;

import games.sparking.altara.redis.packet.Packet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerMessagePacket extends Packet {

    private final UUID player;
    private final List<String> message;

    public PlayerMessagePacket(UUID player, String message) {
        this.player = player;
        this.message = Collections.singletonList(message);
    }

    public PlayerMessagePacket(UUID player, String... message) {
        this.player = player;
        this.message = Arrays.asList(message);
    }

    @Override
    public void receive() {
        Player player = Bukkit.getPlayer(this.player);
        if (player != null) {
            message.forEach(player::sendMessage);
        }
    }

}
