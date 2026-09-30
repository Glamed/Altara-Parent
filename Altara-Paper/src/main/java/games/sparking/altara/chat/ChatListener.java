package games.sparking.altara.chat;

import games.sparking.altara.Altara;
import games.sparking.altara.chat.impl.ShadowMuteChannel;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Routes all chat through the {@link ChatChannel} system.
 *
 * <ul>
 *   <li>{@link AsyncChatEvent} HIGHEST — resolves the channel (usable prefix → one-off,
 *       otherwise the player's active channel), dispatches, and cancels the vanilla
 *       broadcast.</li>
 *   <li>{@link PlayerJoinEvent} MONITOR — restores the player's saved channel.</li>
 *   <li>{@link PlayerQuitEvent} MONITOR — evicts the in-memory channel entry.</li>
 * </ul>
 */
public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim();
        event.setCancelled(true);
        if (message.isEmpty()) return;

        ChatChannel channel = ChatChannelRegistry.getByPrefix(sender, message);
        if (channel != null) {
            message = message.substring(channel.getPrefix().length()).trim();
            if (message.isEmpty()) return;
        } else {
            channel = ChatService.getChatChannel(sender);
            // Permission may have been revoked since the channel was selected.
            if (!channel.canUse(sender)) {
                ChatService.resetChatChannel(sender);
                channel = ChatService.getChatChannel(sender);
            }
        }

        // Profiler-flagged players are shadow-muted regardless of the channel they picked.
        if (Altara.getSharedInstance().getProfilerService().isShadowMuted(sender.getUniqueId())) {
            channel = ShadowMuteChannel.getInstance();
        }

        channel.dispatch(sender, message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        ChatService.loadChatChannel(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        ChatService.removePlayer(event.getPlayer().getUniqueId());
    }
}
