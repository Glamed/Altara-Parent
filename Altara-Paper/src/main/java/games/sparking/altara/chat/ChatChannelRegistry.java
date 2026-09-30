package games.sparking.altara.chat;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static registry that maps channel names and prefixes to {@link ChatChannel}
 * instances.  Channels are checked in registration order when resolving a prefix.
 */
public final class ChatChannelRegistry {

    private static final List<ChatChannel> CHANNELS = new CopyOnWriteArrayList<>();

    private ChatChannelRegistry() {}

    public static void register(ChatChannel channel) {
        CHANNELS.add(channel);
    }

    /** The channel with the given (case-insensitive) name, or {@code null}. */
    public static ChatChannel getByName(String name) {
        if (name == null) return null;
        for (ChatChannel c : CHANNELS)
            if (c.getName().equalsIgnoreCase(name))
                return c;
        return null;
    }

    /**
     * The first channel {@code player} may use whose prefix starts {@code message},
     * or {@code null}.  Prefixes of channels the player can't use are ignored, so the
     * message is sent normally instead.
     */
    public static ChatChannel getByPrefix(Player player, String message) {
        for (ChatChannel c : CHANNELS) {
            if (c.getPrefix() != null && message.startsWith(c.getPrefix()) && c.canUse(player))
                return c;
        }
        return null;
    }

    public static List<ChatChannel> getChannels() {
        return Collections.unmodifiableList(CHANNELS);
    }
}
