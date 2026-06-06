package games.sparking.altara.chat;

import org.bukkit.entity.Player;

/**
 * A {@link ChatChannel} whose audience is gated behind a Bukkit permission node.
 *
 * <p>Extend this class when you want a channel that only players with a specific
 * permission can <em>see</em>.  Sending is handled separately (e.g. via
 * {@link games.sparking.altara.chat.command.ChatCommands} or a prefix check in
 * {@link ChatListener}).
 *
 * <h3>Adding a new filtered channel — three steps</h3>
 * <ol>
 *   <li>Create a singleton subclass of {@code FilteredChatChannel}.</li>
 *   <li>Implement {@link #format} and optionally override
 *       {@link games.sparking.altara.chat.ChatChannel#dispatch}.</li>
 *   <li>Call {@link ChatChannelRegistry#register(ChatChannel)} in
 *       {@code AltaraPaper.registerChatChannels()}.</li>
 * </ol>
 */
public abstract class FilteredChatChannel extends ChatChannel {

    /** Bukkit permission required to receive messages in this channel. */
    private final String receivePermission;

    protected FilteredChatChannel(String name, String prefix,
                                   boolean log, boolean global,
                                   boolean persistable,
                                   String receivePermission) {
        super(name, prefix, log, global, persistable);
        this.receivePermission = receivePermission;
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return viewer.hasPermission(receivePermission);
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return viewer.hasPermission(receivePermission);
            }
        };
    }
}

