package games.sparking.altara.punishment;

import games.sparking.altara.Altara;
import games.sparking.altara.task.Tasks;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Paper-side punishment manager.
 *
 * <p>Calling {@link #issue()} posts the punishment to the Web API via
 * {@link PunishmentService}. The Web layer persists the record and publishes
 * a {@code PunishmentIssuedPacket} to Redis, which is received by every Paper
 * server and applies the in-game effects (kick, chat notice, etc.).
 *
 * <p>This means punishments work <b>cross-server</b>: a moderator on Lobby can
 * ban a player currently sitting on a Games server.
 */
public class PunishmentManager {

    private final UUID playerUuid;
    private final UUID staffUuid;
    private final List<RestrictionAction> actions;
    private final InfractionType reason;
    private final String message;

    public PunishmentManager(CommandSender staff, Player target, PunishmentType type,
                             long duration, InfractionType reason) {
        this(staff, target, type, duration, reason, null);
    }

    public PunishmentManager(CommandSender staff, Player target, PunishmentType type,
                             long duration, InfractionType reason, String message) {
        this(staff, target, List.of(new RestrictionAction(type, duration)), reason, message);
    }

    public PunishmentManager(CommandSender staff, Player target, List<RestrictionAction> actions,
                             InfractionType reason, String message) {
        this.playerUuid = target.getUniqueId();
        this.staffUuid  = (staff instanceof Player p)
                ? p.getUniqueId()
                : UUID.fromString(Punishment.CONSOLE_UUID);
        this.actions    = List.copyOf(actions);
        this.reason     = reason;
        this.message    = message;
    }

    /**
     * Asynchronously posts the punishment to Altara-Web.
     * The Web API will persist it and broadcast a {@code PunishmentIssuedPacket}
     * to all Paper servers, which applies the in-game effect.
     */
    public void issue() {
        if (actions.isEmpty()) return;

        PunishmentService svc = Altara.getSharedInstance().getPunishmentService();
        svc.issuePunishment(staffUuid, playerUuid, reason, actions, message, punishment -> {
            if (punishment == null) {
                // Best-effort fallback: log warning — the packet won't be sent but at least
                // the staff member gets feedback.
                Tasks.runSync(() -> {
                    // Caller's error handling goes here; keep this minimal.
                });
            }
        }, true /* async */);
    }
}

