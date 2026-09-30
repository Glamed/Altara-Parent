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
 * Sent when a staff member bans a flagged player via the profiler ({@code /profilerban}).
 * All Paper servers drop the local flag and notify staff.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ProfilerBanPacket extends Packet {

    private String playerUuid;
    private String playerName;
    private String staffName;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER) return;

        Altara.getSharedInstance().getProfilerService().uncache(UUID.fromString(playerUuid));

        var message = CC.notice("Profiler ban issued.",
                "*" + staffName + "* suspended *" + playerName + "* for a compromised account.");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(ProfilerService.PERMISSION)) staff.sendMessage(message);
        }
    }
}
