package games.sparking.altara.chat.command;

import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.chat.ChatChannelRegistry;
import games.sparking.altara.chat.ChatService;
import games.sparking.altara.chat.impl.StaffChannel;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

/**
 * {@code /channel [name]} switches the active chat channel (or lists channels);
 * {@code /sc <message>} sends to staff chat without switching.
 */
public class ChatCommands {

    @Command(names = {"channel", "ch"},
            description = "Switch your chat channel",
            permission = "player")
    public boolean channel(Player sender, @Param(name = "channel", defaultValue = "") String name) {
        if (name.isEmpty()) {
            listChannels(sender);
            return true;
        }

        ChatChannel channel = ChatChannelRegistry.getByName(name);
        if (channel == null || !channel.isSelectable() || !channel.canUse(sender)) {
            sender.sendMessage(CC.error("Invalid channel.", "Use */channel* to see the channels you can use."));
            return false;
        }

        if (ChatService.getChatChannel(sender) == channel) {
            sender.sendMessage(CC.info("You're already talking in *" + channel.getName() + "*."));
            return false;
        }

        ChatService.setChatChannel(sender, channel, false);
        return true;
    }

    private void listChannels(Player sender) {
        ChatChannel active = ChatService.getChatChannel(sender);

        sender.sendMessage(CC.header("Chat Channels"));
        for (ChatChannel channel : ChatChannelRegistry.getChannels()) {
            if (!channel.isSelectable() || !channel.canUse(sender)) continue;

            boolean current = channel == active;
            Component line = Component.text()
                    .append(Component.text(channel.getName(), current ? Theme.PRIMARY : Theme.TEXT_STRONG))
                    .append(channel.getPrefix() == null ? Component.empty()
                            : Component.text(" (prefix " + channel.getPrefix() + ")", Theme.TEXT))
                    .append(current ? Component.text(" (current)", Theme.TEXT) : Component.empty())
                    .hoverEvent(HoverEvent.showText(Component.text("Click to select this channel.", Theme.TEXT)))
                    .clickEvent(ClickEvent.runCommand("/channel " + channel.getName()))
                    .build();
            sender.sendMessage(CC.item(line));
        }
        sender.sendMessage(CC.footer());
    }

    @Command(names = {"sc", "staffchat"},
            description = "Send a message to staff chat",
            permission = StaffChannel.PERMISSION,
            playerOnly = true)
    public boolean staffChat(Player sender, @Param(name = "message", wildcard = true) String message) {
        StaffChannel.getInstance().dispatch(sender, message);
        return true;
    }
}
