package games.sparking.altara.punishment.listeners;

import games.sparking.altara.Altara;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentService;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class PunishmentListener implements Listener {

    // ── Chat ───────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PunishmentService service = Altara.getSharedInstance().getPunishmentService();

        if (!service.isChatMuted(player.getUniqueId())) return;

        event.setCancelled(true);

        // Find the punishment that carries the active chat restriction so we can show the remaining time.
        Punishment mute = service.getActivePunishments(player.getUniqueId()).stream()
                .filter(p -> p.hasActiveRestriction(PunishmentType.CHAT_RESTRICTION))
                .findFirst()
                .orElse(null);

        player.sendMessage(CC.format("<red><bold>Chat Restricted"));

        if (mute != null) {
            long remaining = mute.getRemainingDuration(PunishmentType.CHAT_RESTRICTION);
            boolean permanent = remaining == -1L;
            player.sendMessage(CC.format("<gray>You are unable to send messages "
                    + (permanent ? "indefinitely" : "for <light_purple>" + Time.formatDetailed(remaining) + "<gray>")));
        }
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerUUID = event.getUniqueId();
        PunishmentService service = Altara.getSharedInstance().getPunishmentService();

        // Ensure punishments are loaded for this player (we're already on an async thread).
        service.loadPunishments(playerUUID);

        Punishment ban = service.getActiveBan(playerUUID);
        if (ban == null) return;

        long remaining = ban.getRemainingDuration(PunishmentType.SUSPENSION);
        boolean permanent = remaining == -1L;
        String reasonDisplay = ban.getReason() != null ? ban.getReason().getDisplayName() : "Policy Violation";

        Component kickMessage = CC.format(
                "\n<red>Your account has been suspended"
                + "\n<gray>\"" + reasonDisplay + "<gray>\""
                + "\n\n<gray>This suspension "
                + (permanent
                        ? "will never expire"
                        : "will expire in <red>" + Time.formatDetailed(remaining) + "<gray>")
                + ". Visit <red><u>sparking.games/appeal<gray> to submit an appeal"
        );

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
    }
}
