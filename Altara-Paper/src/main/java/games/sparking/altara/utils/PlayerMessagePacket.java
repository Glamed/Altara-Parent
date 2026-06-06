package games.sparking.altara.utils;

import games.sparking.altara.redis.packet.Packet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerMessagePacket extends Packet {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final UUID player;
    /** Stored as MiniMessage-serialised strings for safe Redis transport. */
    private final List<String> message;

    // ── Component constructors (preferred) ─────────────────────────────────────

    public PlayerMessagePacket(UUID player, Component message) {
        this.player  = player;
        this.message = Collections.singletonList(MM.serialize(message));
    }

    public PlayerMessagePacket(UUID player, Component... messages) {
        this.player  = player;
        this.message = Arrays.stream(messages).map(MM::serialize).toList();
    }

    // ── String constructors (kept for back-compat) ─────────────────────────────

    public PlayerMessagePacket(UUID player, String message) {
        this.player  = player;
        this.message = Collections.singletonList(message);
    }

    public PlayerMessagePacket(UUID player, String... message) {
        this.player  = player;
        this.message = Arrays.asList(message);
    }

    @Override
    public void receive() {
        Player p = Bukkit.getPlayer(this.player);
        if (p == null) return;
        for (String msg : message) {
            // Try to deserialise as MiniMessage; fall back to plain string.
            try {
                p.sendMessage(MM.deserialize(msg));
            } catch (Exception e) {
                p.sendMessage(msg);
            }
        }
    }
}
