package games.sparking.altara.profiler.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.redis.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Published by the Paper server that first detects a flagged account joining.
 * Every Paper server that receives this packet will:
 * <ol>
 *   <li>Register the player in its local {@link ProfilerService} (shadow-mute them).</li>
 *   <li>Broadcast the profiler alert to all online staff.</li>
 * </ol>
 */
@AllArgsConstructor
@NoArgsConstructor
public class ProfilerFlagPacket extends Packet {

    private String playerUuid;
    private String playerName;
    private int    score;
    private int    compromisedAltCount;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER) return;

        UUID uuid = UUID.fromString(playerUuid);
        ProfilerService svc = Altara.getSharedInstance().getProfilerService();

        // Register (or refresh) the shadow-muted record on this server.
        // The channel switch is handled per-server by ProfilerListener on PlayerJoinEvent.
        svc.flag(uuid, playerName, score, compromisedAltCount);

        // Notify all online staff
        broadcastToStaff();
    }

    private void broadcastToStaff() {
        // Build hover text showing the compromised-alt count
        Component hoverText = Component.text()
                .append(Component.text("Compromised alt accounts: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(compromisedAltCount), NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Internal Score: ", NamedTextColor.GRAY))
                .append(Component.text(String.valueOf(score), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(altCountAdvice(compromisedAltCount))
                .build();

        Component nameComponent = Component.text(playerName, NamedTextColor.YELLOW, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(hoverText));

        Component alert = Component.text()
                .append(Component.text("[PROFILER] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(nameComponent)
                .append(Component.text(" logged in with a suspected compromised account. ", NamedTextColor.YELLOW))
                .append(Component.text("[hover username for details]", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))
                .build();

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(ProfilerService.PERMISSION)) {
                staff.sendMessage(alert);
            }
        }
        Bukkit.getConsoleSender().sendMessage("[PROFILER] " + playerName + " flagged (score=" + score + ", alts=" + compromisedAltCount + ")");
    }

    private static Component altCountAdvice(int count) {
        if (count >= 300) {
            return Component.text("300+ alts → You may punish this account directly (Mod+).", NamedTextColor.RED, TextDecoration.BOLD);
        } else if (count >= 150) {
            return Component.text("150-299 alts → Request an IP check from an Admin (#ip-check).", NamedTextColor.GOLD);
        } else {
            return Component.text("0-149 alts → Monitor, no special action required.", NamedTextColor.GREEN);
        }
    }
}


