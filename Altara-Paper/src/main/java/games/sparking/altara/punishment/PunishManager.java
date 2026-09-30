package games.sparking.altara.punishment;

import games.sparking.altara.Altara;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Issues a punishment through the Web API.  The API persists it and publishes a
 * {@link games.sparking.altara.punishment.packet.PunishmentIssuedPacket}, which enforces
 * it on whichever server the player is on.
 */
public class PunishManager {

    private final Player staff;
    private final PunishTarget target;
    private final List<RestrictionAction> actions;
    private final InfractionType reason;
    private final String message;

    public PunishManager(Player staff, PunishTarget target, List<RestrictionAction> actions,
                         InfractionType reason, String message) {
        this.staff   = staff;
        this.target  = target;
        this.actions = new ArrayList<>(actions);
        this.reason  = reason;
        this.message = message;
    }

    /** Issues asynchronously and reports the outcome to the staff member. */
    public void issue() {
        if (actions.isEmpty()) return;

        Altara.getSharedInstance().getPunishmentService().issuePunishment(
                staff.getUniqueId(), target.uuid(), reason, actions, message,
                punishment -> staff.sendMessage(punishment == null
                        ? CC.error("Unable to punish.", "The punishment couldn't be saved. Please try again.")
                        : CC.success("Punishment issued.", "*" + target.name() + "* was punished for *"
                                + reason.getDisplayName() + "*.")),
                true);
    }
}
