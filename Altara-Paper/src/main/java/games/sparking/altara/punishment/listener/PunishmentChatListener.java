package games.sparking.altara.punishment.listener;

import games.sparking.altara.Altara;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Silences chat for players who have an active {@code CHAT_RESTRICTION} punishment.
 *
 * <p>Runs at {@link EventPriority#LOW} so the main {@code ChatListener} (HIGHEST)
 * never sees the event when it is cancelled here.
 */
public class PunishmentChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        boolean muted = Altara.getSharedInstance().getPunishmentService()
                .isChatMuted(event.getPlayer().getUniqueId());

        if (muted) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cYou are currently chat-restricted and cannot send messages."));
        }
    }
}

