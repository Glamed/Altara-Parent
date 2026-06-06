package games.sparking.altara.punishment;

import games.sparking.altara.Altara;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PunishManager {

    private final UUID playerUUID;
    private final UUID staffUUID;
    private final List<RestrictionAction> actions;
    private final InfractionType reason;
    private final String message;

    public PunishManager(Player staff, Player target, PunishmentType type, long duration, InfractionType reason) {
        this(staff, target, type, duration, reason, null);
    }

    public PunishManager(Player staff, Player target, PunishmentType type, long duration, InfractionType reason, String message) {
        this(staff, target, List.of(new RestrictionAction(type, duration)), reason, message);
    }

    public PunishManager(Player staff, Player target, List<RestrictionAction> actions, InfractionType reason, String message) {
        this.playerUUID = target.getUniqueId();
        this.staffUUID  = staff.getUniqueId();
        this.actions    = new ArrayList<>(actions);
        this.reason     = reason;
        this.message    = message;
    }

    /**
     * Issues the punishment asynchronously.
     *
     * <p>Flow:
     * <ol>
     *   <li>POST to {@code /api/punishment} via the Web REST API.</li>
     *   <li>Web persists to MongoDB and publishes a {@link games.sparking.altara.punishment.packet.PunishmentIssuedPacket} via Redis.</li>
     *   <li>All Paper servers receive the packet and enforce locally (kick / notify).</li>
     * </ol>
     */
    public void issue() {
        if (actions.isEmpty()) return;
        Altara.getSharedInstance().getPunishmentService()
              .issuePunishment(staffUUID, playerUUID, reason, actions, message, null, true);
    }
}
