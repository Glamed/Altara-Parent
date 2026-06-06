package games.sparking.altara.chat;

import games.sparking.altara.chat.impl.GlobalChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the active {@link ChatChannel} for each online player.
 *
 * <p>The active channel is the one used when a player types without a prefix.
 * It is persisted via {@link AltaraSettings#ACTIVE_CHANNEL} (which is
 * {@code storedInProfile = true}) so the choice survives restarts and server
 * switches.
 */
public final class ChatService {

    /** In-memory channel map (populated on join, cleared on quit). */
    private static final Map<UUID, ChatChannel> CHANNELS = new HashMap<>();

    private ChatService() {}

    // ── Channel management ────────────────────────────────────────────────────

    /**
     * Sets {@code player}'s active channel.
     *
     * @param player  the player
     * @param channel the target channel
     * @param silent  if {@code true} no notification is sent to the player
     */
    public static void setChatChannel(Player player, ChatChannel channel, boolean silent) {
        CHANNELS.put(player.getUniqueId(), channel);

        // Persist the preference unless the channel is non-persistable (e.g. shadow-mute).
        if (channel.isPersistable()) {
            AltaraSettings.ACTIVE_CHANNEL.set(player, channel.getName());
        }

        if (!silent) {
            player.sendMessage(CC.text(
                    "You are now in the " + channel.getName() + " channel.",
                    NamedTextColor.YELLOW));
        }
    }

    /**
     * Loads the channel that was last saved in {@code uuid}'s profile preferences
     * and applies it in-memory.  Falls back to {@link GlobalChannel} if no
     * preference is stored or the saved channel name no longer exists.
     *
     * <p>Must be called on the main thread (or at least after the Player object
     * exists), e.g. from {@link org.bukkit.event.player.PlayerJoinEvent}.
     */
    public static void loadChatChannel(UUID uuid) {
        Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player == null) return;

        String saved  = AltaraSettings.ACTIVE_CHANNEL.get(player);
        ChatChannel ch = ChatChannelRegistry.getByName(saved);

        // If the saved channel is non-persistable we shouldn't restore it.
        if (ch == null || !ch.isPersistable()) ch = GlobalChannel.getInstance();

        CHANNELS.put(uuid, ch);
    }

    /**
     * Returns {@code player}'s current active channel, defaulting to
     * {@link GlobalChannel} if none is set.
     */
    public static ChatChannel getChatChannel(Player player) {
        return CHANNELS.getOrDefault(player.getUniqueId(), GlobalChannel.getInstance());
    }

    /** Removes the in-memory channel entry when a player quits. */
    public static void removePlayer(UUID uuid) {
        CHANNELS.remove(uuid);
    }
}

