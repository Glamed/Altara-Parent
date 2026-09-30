package games.sparking.altara.utils;

import games.sparking.altara.redis.packet.Packet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Delivers chat components to a player on whichever server they are connected to. */
public class PlayerMessagePacket extends Packet {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final UUID player;
    /** MiniMessage-serialised components for Redis transport. */
    private final List<String> message;

    public PlayerMessagePacket(UUID player, Component... messages) {
        this.player  = player;
        this.message = Arrays.stream(messages).map(MM::serialize).toList();
    }

    @Override
    public void receive() {
        Player p = Bukkit.getPlayer(this.player);
        if (p == null || message == null) return;
        for (String msg : message) {
            p.sendMessage(MM.deserialize(msg));
        }
    }
}
