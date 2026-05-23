package games.sparking.altara.profile.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.redis.packet.Packet;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class ProfileUpdatePacket extends Packet {

    private final UUID uuid;

    @Override
    public void receive() {
        if (Altara.getSystemType() == SystemType.WEB) {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
            if (profile == null) {
                return;
            }

            Altara.getSharedInstance().getProfileService().updateProfile(uuid, unused -> {
            }, true);
        }
    }

}
