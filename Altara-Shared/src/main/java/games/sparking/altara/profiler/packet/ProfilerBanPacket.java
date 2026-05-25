package games.sparking.altara.profiler.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Sent when a staff member bans a flagged player via the profiler ({@code /profilerban}).
 * All Paper servers remove the record from their local cache and notify staff.
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

        UUID uuid = UUID.fromString(playerUuid);
        Altara.getSharedInstance().getProfilerService().remove(uuid);

        Component msg = Component.text()
                .append(Component.text("[PROFILER] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(staffName, NamedTextColor.AQUA))
                .append(Component.text(" banned ", NamedTextColor.GRAY))
                .append(Component.text(playerName, NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" for Compromised Account.", NamedTextColor.GRAY))
                .build();

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(ProfilerService.PERMISSION)) {
                staff.sendMessage(msg);
            }
        }
    }
}

