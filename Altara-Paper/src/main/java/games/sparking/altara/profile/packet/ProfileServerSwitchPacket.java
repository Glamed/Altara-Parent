package games.sparking.altara.profile.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.SystemType;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * Published by a server when one of its players disconnects (whether switching servers or
 * leaving the network entirely). Other Paper servers that receive this packet refresh their
 * cached copy of the profile from the API so that {@code lastServer} and related fields
 * stay in sync across the network.
 *
 * <p>The {@code fromServer} field records which server the player left so receivers can
 * ignore the packet if it originated from themselves.
 */
@NoArgsConstructor
@AllArgsConstructor
public class ProfileServerSwitchPacket extends Packet {

    private UUID playerUuid;
    private String fromServer;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER) return;

        // Ignore packets we published ourselves.
        if (Altara.getSharedInstance().getLocalServerName().equals(fromServer)) return;

        // If the player is now online on this server, mark them as a confirmed switch so the
        // quit handler on THIS server knows not to treat their eventual disconnect as a full
        // network-quit when they next leave.
        if (Bukkit.getPlayer(playerUuid) != null) {
            AltaraPaper.getPaperInstance().markServerSwitch(playerUuid);
        }

        // Refresh the cached profile so session fields (lastServer, lastSeen, etc.) are up to date.
        Profile cached = Altara.getSharedInstance().getProfileService().getProfile(playerUuid);
        if (cached != null && Bukkit.getPlayer(playerUuid) == null) {
            // Player isn't online here — evict the stale cache so the next load hits the API.
            Altara.getSharedInstance().getProfileService().removeProfile(playerUuid);
        }
    }
}


