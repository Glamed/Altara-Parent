package games.sparking.altara.chat;

import org.bukkit.entity.Player;
import java.util.List;

public abstract class FilteredChatChannel extends ChatChannel {

    private final boolean ignoreChatRestrictions;

    public FilteredChatChannel(String name,
                               String displayName,
                               String permission,
                               List<String> aliases,
                               char prefix,
                               int priority) {
        this(name, displayName, permission, aliases, prefix, priority, false);
    }

    public FilteredChatChannel(String name,
                               String displayName,
                               String permission,
                               List<String> aliases,
                               char prefix,
                               int priority,
                               boolean ignoreChatRestrictions) {
        super(name, displayName, permission, aliases, prefix, priority);
        this.ignoreChatRestrictions = ignoreChatRestrictions;
    }

    public static boolean canChat(Player player,
                                  String message,
                                  ChatChannel chatChannel,
                                  boolean ignoreChatRestrictions) {


        return true;
    }

    @Override
    public boolean onChat(Player player, String message) {
        return canChat(player, message, this, ignoreChatRestrictions);
    }
}