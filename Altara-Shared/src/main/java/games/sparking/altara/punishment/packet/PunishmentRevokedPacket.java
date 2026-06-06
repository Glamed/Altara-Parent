package games.sparking.altara.punishment.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Published by the Web API whenever a {@link games.sparking.altara.punishment.Punishment}
 * is soft-deleted (revoked) via {@code DELETE /api/punishment/{id}}.
 *
 * <p>Receipt is a no-op on non-Paper nodes. On Paper the record is removed from the
 * local cache so active-check hot-paths (mute, ban) immediately reflect the revocation.
 */
@AllArgsConstructor
@NoArgsConstructor
public class PunishmentRevokedPacket extends Packet {

    private String punishmentId;
    private String playerUuid;
    private String revokedBy;

    @Override
    public void receive() {
        // Only Paper servers maintain a local punishment cache that needs updating.
        if (Altara.getSystemType() != SystemType.PAPER) return;
        if (punishmentId == null || playerUuid == null) return;

        UUID uuid;
        try {
            uuid = UUID.fromString(playerUuid);
        } catch (IllegalArgumentException e) {
            return;
        }

        Altara.getSharedInstance()
              .getPunishmentService()
              .removeFromCacheFromPacket(punishmentId, uuid);
    }
}

