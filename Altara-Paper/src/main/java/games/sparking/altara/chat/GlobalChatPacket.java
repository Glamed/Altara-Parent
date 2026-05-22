package games.sparking.altara.chat;

import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.redis.packet.Packet;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * A Redis packet that delivers a global (cross-server) chat message to every
 * Paper server on the network. Each server individually filters delivery based
 * on the recipient's {@link AltaraSettings#GLOBAL_CHAT} and
 * {@link AltaraSettings#ALL_CHAT} settings.
 */
@RequiredArgsConstructor
public class GlobalChatPacket extends Packet {

    /** The fully-formatted, color-translated message string ready to display. */
    private final String formattedMessage;

    @Override
    public void receive() {
        // Always log to console
        Bukkit.getConsoleSender().sendMessage(formattedMessage);

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players with all chat disabled
            if (!AltaraSettings.ALL_CHAT.get(player)) continue;
            // Skip players with global chat disabled
            if (!AltaraSettings.GLOBAL_CHAT.get(player)) continue;

            player.sendMessage(formattedMessage);
        }
    }
}


