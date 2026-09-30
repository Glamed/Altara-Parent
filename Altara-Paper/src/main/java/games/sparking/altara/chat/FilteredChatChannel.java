package games.sparking.altara.chat;

import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * A {@link ChatChannel} gated behind a Bukkit permission node: only players with
 * the permission may send to it, switch to it, or receive its messages.
 *
 * <h3>Adding a new filtered channel</h3>
 * <ol>
 *   <li>Create a singleton subclass of {@code FilteredChatChannel}.</li>
 *   <li>Implement {@link #format}.  Override {@link #getAudience()} only to add extra
 *       conditions — keep the permission check via {@link #hasAccess(Player)}.</li>
 *   <li>Call {@link ChatChannelRegistry#register(ChatChannel)} in
 *       {@code AltaraPaper.registerChatChannels()}.</li>
 * </ol>
 */
@Getter
public abstract class FilteredChatChannel extends ChatChannel {

    private final String permission;

    protected FilteredChatChannel(String name, String prefix, boolean log, boolean global,
                                  boolean selectable, String permission) {
        super(name, prefix, log, global, selectable);
        this.permission = permission;
    }

    public boolean hasAccess(Player player) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean canUse(Player player) {
        return hasAccess(player);
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return hasAccess(viewer);
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return hasAccess(viewer);
            }
        };
    }
}
