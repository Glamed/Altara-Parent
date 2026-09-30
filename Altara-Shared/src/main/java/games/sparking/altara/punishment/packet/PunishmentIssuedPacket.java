package games.sparking.altara.punishment.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentNotifier;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.task.Tasks;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Published by the Web API whenever a new {@link Punishment} is persisted.  On Paper the
 * local cache is updated and, if the player is online here, the punishment is enforced and
 * shown immediately.  Players who are offline are told on their next join.
 */
@AllArgsConstructor
@NoArgsConstructor
public class PunishmentIssuedPacket extends Packet {

    private Punishment punishment;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER || punishment == null) return;

        Altara.getSharedInstance().getPunishmentService().updateCacheFromPacket(punishment);
        Tasks.run(this::enforceLocally);
    }

    private void enforceLocally() {
        UUID playerUuid;
        try {
            playerUuid = UUID.fromString(punishment.getPlayerUuid());
        } catch (IllegalArgumentException | NullPointerException e) {
            return;
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        // Offline here: whichever server they join next shows it (see PunishmentListener).
        PunishmentNotifier.deliver(player, punishment);
    }
}
