package games.sparking.altara.chat.command;

import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.chat.ChatChannelRegistry;
import games.sparking.altara.chat.ChatService;
import games.sparking.altara.chat.impl.AdminChannel;
import games.sparking.altara.chat.impl.StaffChannel;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

/**
 * Commands for switching and sending to chat channels.
 *
 * <ul>
 *   <li>{@code /channel [name]} / {@code /ch [name]} — switch active channel or list
 *       available channels.</li>
 *   <li>{@code /sc <message>} — quick-send to {@link StaffChannel} without switching.</li>
 *   <li>{@code /ac <message>} — quick-send to {@link AdminChannel} without switching.</li>
 * </ul>
 */
public class ChatCommands {

    // ── /channel ───────────────────────────────────────────────────────────────

    @Command(names = {"channel", "ch"},
            description = "Switch your active chat channel",
            permission = "player")
    public boolean channel(Player sender,
                           @Param(name = "channel", defaultValue = "") String name) {
        if (name.isEmpty()) {
            // List available channels.
            sender.sendMessage(CC.CHAT_BAR);
            sender.sendMessage(CC.format("<yellow><bold>Chat Channels</bold></yellow>"));
            for (ChatChannel ch : ChatChannelRegistry.getChannels()) {
                if (ch.getPrefix() == null) continue; // system-only
                boolean active = ChatService.getChatChannel(sender).getName()
                        .equalsIgnoreCase(ch.getName());
                sender.sendMessage(CC.format(
                        (active ? "<green>▶ " : "  ") + "<yellow>" + ch.getName() +
                        (ch.getPrefix() != null ? " <gray>(" + ch.getPrefix() + ")" : "")));
            }
            sender.sendMessage(CC.CHAT_BAR);
            return true;
        }

        ChatChannel channel = ChatChannelRegistry.getByName(name);
        if (channel == null) {
            sender.sendMessage(CC.format("<red>Unknown channel <yellow>" + name + "<red>."));
            return false;
        }

        if (channel.getPrefix() == null) {
            sender.sendMessage(CC.format("<red>You cannot switch to that channel."));
            return false;
        }

        ChatService.setChatChannel(sender, channel, false);
        return true;
    }

    // ── /sc ────────────────────────────────────────────────────────────────────

    @Command(names = {"sc", "staffchat"},
            description = "Send a message to staff chat",
            permission = "altara.staff")
    public boolean staffChat(Player sender,
                             @Param(name = "message", wildcard = true) String message) {
        StaffChannel.getInstance().dispatch(sender, message);
        return true;
    }

    // ── /ac ────────────────────────────────────────────────────────────────────

    @Command(names = {"ac", "adminchat"},
            description = "Send a message to admin chat",
            permission = "altara.admin")
    public boolean adminChat(Player sender,
                             @Param(name = "message", wildcard = true) String message) {
        AdminChannel.getInstance().dispatch(sender, message);
        return true;
    }
}

