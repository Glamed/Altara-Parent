package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChatService;
import games.sparking.altara.chat.FilteredChatChannel;
import games.sparking.altara.chat.GlobalChatPacket;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class PublicChat extends FilteredChatChannel {

    public PublicChat() {
        super("public",
                CC.RED + "Public",
                null,
                Arrays.asList("pc", "p", "pub", "global", "g", "gc"),
                '!',
                0);
    }


    /**
     * Override the default local-only broadcast with a cross-server Redis publish.
     * The {@link GlobalChatPacket} is received on every Paper server and each
     * server filters delivery per-player via the GLOBAL_CHAT / ALL_CHAT settings.
     */
    @Override
    public void chat(Player player, String message) {
        if (!onChat(player, message)) return;

        // Build the formatted message string on this server using the sender's prefix.
        String prefix = ChatService.getPrefixGetter().apply(player, Bukkit.getConsoleSender());
        String formatted = prefix + CC.format(" &8&l» ") + getChatColor(player) + message;

        // Publish to the entire network via Redis pub/sub.
        new GlobalChatPacket(formatted).publish();
    }

    @Override
    public String getFormat(Player player, CommandSender sender) {
        if (sender instanceof Player && !AltaraSettings.GLOBAL_CHAT.get((Player) sender))
            return null;

        return "%1$s" + CC.format(" &8&l» ") + getChatColor(player) + "%2$s";
    }

    public String getChatColor(Player player) {
        return "&f";
    }
}