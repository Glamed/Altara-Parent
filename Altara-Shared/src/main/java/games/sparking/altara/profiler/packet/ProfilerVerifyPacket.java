package games.sparking.altara.profiler.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.CC;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Sent when a staff member verifies a flagged player ({@code /profilerverify}).
 * All Paper servers drop the local flag and notify staff.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ProfilerVerifyPacket extends Packet {

    private String playerUuid;
    private String playerName;
    private String staffName;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER) return;

        // The issuing server already persisted the verification — just drop the local cache.
        Altara.getSharedInstance().getProfilerService().uncache(UUID.fromString(playerUuid));

        var message = CC.success("Profiler flag cleared.", "*" + staffName + "* verified *" + playerName + "*.");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(ProfilerService.PERMISSION)) staff.sendMessage(message);
        }
    }
}
