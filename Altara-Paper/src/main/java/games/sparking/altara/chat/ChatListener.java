package games.sparking.altara.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Routes all chat events through the {@link ChatChannel} system.
 *
 * <ul>
 *   <li>{@link AsyncPlayerChatEvent HIGHEST} — intercepts raw messages, resolves
 *       the target channel (prefix → one-off, otherwise player's active channel),
 *       dispatches through {@link ChatChannel#dispatch}, and cancels the event so
 *       Paper's default formatting never fires.</li>
 *   <li>{@link PlayerJoinEvent MONITOR} — restores the player's saved channel.</li>
 *   <li>{@link PlayerQuitEvent MONITOR} — evicts the in-memory channel entry.</li>
 * </ul>
 */
public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender  = event.getPlayer();
        String message = event.getMessage();

        // Cancel the vanilla event — we handle all formatting ourselves.
        event.setCancelled(true);

        // Check if the message starts with a registered channel prefix for a one-off send.
        ChatChannel channel = ChatChannelRegistry.getByPrefix(message);
        if (channel != null) {
            // Strip the prefix before dispatching.
            String stripped = message.substring(channel.getPrefix().length()).trim();
            if (!stripped.isEmpty()) {
                channel.dispatch(sender, stripped);
            }
            return;
        }

        // Otherwise use the player's active channel.
        ChatService.getChatChannel(sender).dispatch(sender, message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        ChatService.loadChatChannel(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        ChatService.removePlayer(event.getPlayer().getUniqueId());
    }
}

