package games.sparking.altara.punishment.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Published by <b>Altara-Web</b> after revoking / removing a punishment.
 *
 * <p>On reception every Paper node removes the punishment from its local cache.
 * If the player is online and the revoked punishment was a ban (now lifted)
 * they receive a notification.
 */
@AllArgsConstructor
@NoArgsConstructor
public class PunishmentRevokedPacket extends Packet {

    private String punishmentId;
    private String playerUuid;
    /** UUID of the staff member who revoked the punishment (nullable for automated revocations). */
    private String revokedBy;

    @Override
    public void receive() {
        // Evict from local punishment cache on all nodes
        if (Altara.getSystemType() != SystemType.WEB) {
            Altara.getSharedInstance().getPunishmentService().removeFromCacheFromPacket(punishmentId, UUID.fromString(playerUuid));
        }

        if (Altara.getSystemType() != SystemType.PAPER) return;

        Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
        if (player == null) return;

        player.sendMessage(MiniMessage.miniMessage().deserialize(
                "<green>A moderation action against your account has been lifted."));
    }
}
