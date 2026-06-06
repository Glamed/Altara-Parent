package games.sparking.altara.chat.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.chat.ChatChannelRegistry;
import games.sparking.altara.redis.packet.Packet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Redis packet that relays a formatted chat message to every other server in
 * the network.
 *
 * <p>When a player sends a message in a global {@link ChatChannel} their home
 * server does local delivery immediately, then publishes this packet.  Every
 * other server that receives it resolves the channel, applies the correct
 * {@link games.sparking.altara.chat.ChannelAudience#canSeeRemote audience check},
 * and delivers the pre-formatted component to eligible players.
 *
 * <p>The packet is silently ignored on the server that originally published it
 * (identified by {@link #originServer}).
 */
public class ChatMessagePacket extends Packet {

    /** MiniMessage-serialised form of the already-formatted component. */
    private final String serialisedMessage;

    /** Internal name of the channel (used to look up audience rules on remote servers). */
    private final String channelName;

    /** UUID of the original sender — stored for future spy / ignore integrations. */
    private final String senderUuid;

    /**
     * Name of the server that sent this packet.  Remote servers skip delivery
     * when their local name matches this value (avoids double-delivery).
     */
    private final String originServer;

    // ── Constructors ───────────────────────────────────────────────────────────

    public ChatMessagePacket(Component message, String channelName,
                             UUID senderUuid, String originServer) {
        this.serialisedMessage = MiniMessage.miniMessage().serialize(message);
        this.channelName       = channelName;
        this.senderUuid        = senderUuid.toString();
        this.originServer      = originServer;
    }

    // ── Receiving ──────────────────────────────────────────────────────────────

    @Override
    public void receive() {
        // Ignore the packet on the server that originally sent it.
        if (originServer != null
                && originServer.equals(Altara.getSharedInstance().getLocalServerName())) {
            return;
        }

        Component component = MiniMessage.miniMessage().deserialize(serialisedMessage);
        ChatChannel channel = ChatChannelRegistry.getByName(channelName);

        if (channel == null) {
            // Unknown channel — log and skip.
            Altara.getSharedInstance().getLogger()
                    .warn("[ChatMessagePacket] Received message for unknown channel: " + channelName);
            return;
        }

        // Console always receives every cross-server message.
        Bukkit.getConsoleSender().sendMessage(component);

        // Deliver to eligible online players using the remote audience check.
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (channel.getAudience().canSeeRemote(viewer, channel)) {
                viewer.sendMessage(component);
            }
        }
    }
}

