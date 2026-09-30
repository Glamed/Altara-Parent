package games.sparking.altara.chat;

import games.sparking.altara.chat.impl.GlobalChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the active {@link ChatChannel} for each online player.
 *
 * <p>The active channel is the one used when a player types without a prefix.
 * It is persisted via {@link AltaraSettings#ACTIVE_CHANNEL} so the choice survives
 * restarts and server switches.  Read from the async chat thread, so thread-safe.
 */
public final class ChatService {

    private static final Map<UUID, ChatChannel> CHANNELS = new ConcurrentHashMap<>();

    private ChatService() {}

    /**
     * Sets {@code player}'s active channel.  Callers must check
     * {@link ChatChannel#isSelectable()} and {@link ChatChannel#canUse(Player)} first.
     */
    public static void setChatChannel(Player player, ChatChannel channel, boolean silent) {
        CHANNELS.put(player.getUniqueId(), channel);
        AltaraSettings.ACTIVE_CHANNEL.set(player, channel.getName());

        if (!silent) {
            player.sendMessage(CC.notice("Channel switched.", "You're now talking in *" + channel.getName() + "*."));
        }
    }

    /** Switches the player back to {@link GlobalChannel} without notifying them. */
    public static void resetChatChannel(Player player) {
        setChatChannel(player, GlobalChannel.getInstance(), true);
    }

    /**
     * Restores the channel saved in the player's preferences, falling back to
     * {@link GlobalChannel} if it no longer exists or they can no longer use it.
     */
    public static void loadChatChannel(Player player) {
        ChatChannel channel = ChatChannelRegistry.getByName(AltaraSettings.ACTIVE_CHANNEL.get(player));
        if (channel == null || !channel.isSelectable() || !channel.canUse(player)) {
            channel = GlobalChannel.getInstance();
        }
        CHANNELS.put(player.getUniqueId(), channel);
    }

    /** The player's active channel, defaulting to {@link GlobalChannel}. */
    public static ChatChannel getChatChannel(Player player) {
        return CHANNELS.getOrDefault(player.getUniqueId(), GlobalChannel.getInstance());
    }

    public static void removePlayer(UUID uuid) {
        CHANNELS.remove(uuid);
    }
}
