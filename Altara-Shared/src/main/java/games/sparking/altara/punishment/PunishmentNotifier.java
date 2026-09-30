package games.sparking.altara.punishment;

import games.sparking.altara.Altara;
import org.bukkit.entity.Player;

/**
 * Makes sure every punishment reaches its player exactly once — live if they're online when
 * it's issued, otherwise the next time they join.  Paper only; call on the main thread.
 */
public final class PunishmentNotifier {

    private PunishmentNotifier() {}

    /**
     * Enforces and shows {@code punishment} to {@code player}.  A suspension disconnects them
     * (the suspension screen is the notice); anything else is the chat notice.  Punishments
     * they've already seen are only enforced, not shown again.
     */
    public static void deliver(Player player, Punishment punishment) {
        PunishmentService service = Altara.getSharedInstance().getPunishmentService();

        if (punishment.hasActiveRestriction(PunishmentType.SUSPENSION)) {
            if (service.claimNotification(punishment)) service.markNotified(punishment);
            player.kick(PunishmentMessages.suspensionScreen(punishment));
            return;
        }

        if (!service.claimNotification(punishment)) return;
        PunishmentMessages.restrictionNotice(punishment, player.getName()).forEach(player::sendMessage);
        service.markNotified(punishment);
    }

    /** Shows every punishment the player hasn't seen yet — used when they join. */
    public static void deliverPending(Player player) {
        PunishmentService service = Altara.getSharedInstance().getPunishmentService();
        for (Punishment punishment : service.getUnnotified(player.getUniqueId())) {
            if (!player.isOnline()) return;
            deliver(player, punishment);
        }
    }
}
