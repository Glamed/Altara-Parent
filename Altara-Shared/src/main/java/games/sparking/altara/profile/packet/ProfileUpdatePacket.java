package games.sparking.altara.profile.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Published by the Web API whenever a profile document changes (profile saves, grants,
 * grant clears).  Paper servers holding the profile re-read it and re-apply permissions,
 * so changes take effect immediately instead of on the next login.
 */
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdatePacket extends Packet {

    private UUID uuid;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER || uuid == null) return;

        if (Altara.getSharedInstance().getProfileService().refreshProfile(uuid) != null) {
            Altara.getSharedInstance().updatePermissions(uuid);
        }
    }
}
