package games.sparking.altara.punishment.listeners;

import games.sparking.altara.Altara;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentMessages;
import games.sparking.altara.punishment.PunishmentNotifier;
import games.sparking.altara.punishment.PunishmentService;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.task.Tasks;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PunishmentListener implements Listener {

    private static final long NOTICE_DELAY_TICKS = 40L;

    /** Muted players can't chat.  Cancelled events (e.g. answered chat prompts) are left alone. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Punishment mute = activeMute(player.getUniqueId());
        if (mute == null) return;

        event.setCancelled(true);
        PunishmentMessages.chatRestricted(mute, player.getName()).forEach(player::sendMessage);
    }

    /** Refuses logins from suspended players.  Runs after the profile is loaded (LOWEST). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        PunishmentService service = Altara.getSharedInstance().getPunishmentService();
        service.loadPunishments(event.getUniqueId());

        Punishment ban = service.getActiveBan(event.getUniqueId());
        if (ban != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, PunishmentMessages.suspensionScreen(ban));
            // The suspension screen is how they learn about it.
            if (service.claimNotification(ban)) service.markNotified(ban);
        }
    }

    /**
     * Tells the player about anything issued while they were offline (or mid-transfer), once
     * the join spam has settled so it isn't buried.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Tasks.runLater(() -> {
            if (player.isOnline()) PunishmentNotifier.deliverPending(player);
        }, NOTICE_DELAY_TICKS);
    }

    /** The punishment carrying an active chat restriction, or {@code null}. */
    public static Punishment activeMute(UUID uuid) {
        return Altara.getSharedInstance().getPunishmentService().getActivePunishments(uuid).stream()
                .filter(p -> p.hasActiveRestriction(PunishmentType.CHAT_RESTRICTION))
                .findFirst()
                .orElse(null);
    }
}
