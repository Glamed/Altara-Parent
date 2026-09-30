package games.sparking.altara.chatinput;

import games.sparking.altara.AltaraPaper;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Captures chat from players with an active {@link ChatInput} before it reaches the
 * channel system.  Runs before mute checks so muted staff can still answer prompts;
 * the input is handled on the main thread.
 */
public class ChatInputListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatInput<?> input = ChatInput.getInput(player);
        if (input == null) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(), () -> {
            if (player.isOnline() && ChatInput.getInput(player) == input) {
                input.handle(player, message);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChatInput.clear(event.getPlayer().getUniqueId());
    }
}
