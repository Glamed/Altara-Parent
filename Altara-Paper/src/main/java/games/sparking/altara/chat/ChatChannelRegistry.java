package games.sparking.altara.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static registry that maps channel names and prefixes to {@link ChatChannel}
 * instances.
 *
 * <p>Channels are checked in registration order when resolving a prefix, so
 * register higher-priority channels first.
 */
public final class ChatChannelRegistry {

    private static final List<ChatChannel> CHANNELS = new ArrayList<>();

    private ChatChannelRegistry() {}

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register(ChatChannel channel) {
        CHANNELS.add(channel);
    }

    // ── Lookup ─────────────────────────────────────────────────────────────────

    /**
     * Returns the channel with the given (case-insensitive) internal name, or
     * {@code null} if no such channel is registered.
     */
    public static ChatChannel getByName(String name) {
        for (ChatChannel c : CHANNELS)
            if (c.getName().equalsIgnoreCase(name))
                return c;
        return null;
    }

    /**
     * Returns the first registered channel whose prefix matches the start of
     * {@code message}, or {@code null} if no prefix matches.
     */
    public static ChatChannel getByPrefix(String message) {
        for (ChatChannel c : CHANNELS) {
            if (c.getPrefix() != null && message.startsWith(c.getPrefix()))
                return c;
        }
        return null;
    }

    /** Returns an unmodifiable view of all registered channels. */
    public static List<ChatChannel> getChannels() {
        return Collections.unmodifiableList(CHANNELS);
    }
}

