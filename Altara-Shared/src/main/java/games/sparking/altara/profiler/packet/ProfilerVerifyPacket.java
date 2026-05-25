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
 * Sent when a staff member verifies a flagged player ({@code /profilerverify}).
 * All Paper servers remove the shadow-mute and notify staff.
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

        UUID uuid = UUID.fromString(playerUuid);
        // Mark verified in local registry — ProfilerListener.onJoin will not re-apply
        // the channel if the player rejoins, and the channel itself gets cleared by
        // ChatService.loadChatChannel which ProfilerCommand calls directly on this server.
        Altara.getSharedInstance().getProfilerService().verify(uuid);

        Component msg = Component.text()
                .append(Component.text("[PROFILER] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(playerName, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" has been ", NamedTextColor.GRAY))
                .append(Component.text("verified", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" by ", NamedTextColor.GRAY))
                .append(Component.text(staffName, NamedTextColor.AQUA))
                .append(Component.text(". Shadow mute removed.", NamedTextColor.GRAY))
                .build();

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(ProfilerService.PERMISSION)) {
                staff.sendMessage(msg);
            }
        }
    }
}

