package games.sparking.altara.chat;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            return;

        ChatService.loadChatChannel(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // fromPlayer() already resolves and caches the channel (setting a default if
        // none was stored in Redis), so simply calling it here is enough.
        ChatService.fromPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ChatService.removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        String message = event.getMessage();
        ChatChannel channel = ChatService.fromPlayer(player);

        ChatChannel fromPrefix = ChatService.fromPrefix(message.charAt(0));
        if (message.length() > 1
                && fromPrefix != null
                && (fromPrefix.canAccess(player))) {
            channel = fromPrefix;
            message = message.substring(1).trim();
        }

        if (message.isEmpty())
            return;

        if (!channel.canAccess(player)) {
            channel = ChatService.getDefaultChannelProvider().getDefaultChannel(player);
            ChatService.setChatChannel(player, channel, false);
        }

        event.setCancelled(true);
        channel.chat(player, message);
    }

}