package games.sparking.altara.chat;

import java.util.List;

/**
 * A {@link FilteredChatChannel} that is also tagged as server-local via the
 * {@link LocalChatChannel} marker interface.  All filtering behaviour is
 * inherited from {@link FilteredChatChannel}; the marker interface tells
 * {@link ChatService} to persist the player's selection to the per-server
 * Redis key rather than the global one.
 */
public abstract class FilteredLocalChatChannel extends FilteredChatChannel implements LocalChatChannel {

    public FilteredLocalChatChannel(String name, String displayName, String permission,
                                    List<String> aliases, char prefix, int priority) {
        super(name, displayName, permission, aliases, prefix, priority);
    }

    public FilteredLocalChatChannel(String name, String displayName, String permission,
                                    List<String> aliases, char prefix, int priority,
                                    boolean ignoreChatRestrictions) {
        super(name, displayName, permission, aliases, prefix, priority, ignoreChatRestrictions);
    }
}