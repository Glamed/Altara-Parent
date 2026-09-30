package games.sparking.altara.server.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.chat.impl.StaffChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Theme;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * A server's heartbeat.  Receivers store it and, when a known server's state changes,
 * tell their own online staff (each server notifies locally, so there's one message per
 * staff member rather than one per server).
 */
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServerPacket extends Packet {

    private ServerInfo serverInfo;

    @Override
    public void receive() {
        if (serverInfo == null) return;

        ServerInfo previous = ServerInfo.getServerInfo(serverInfo.getName());
        ServerState previousState = previous == null ? null : previous.getState();
        ServerInfo.updateServerInfo(serverInfo);

        if (previousState == null || previousState == serverInfo.getState()) return;
        if (Altara.getSharedInstance().getLocalServerName().equals(serverInfo.getName())) return;

        Component message = CC.notice(Component.text("Server status."), Component.text()
                .append(Component.text(serverInfo.getName() + " is now ", Theme.TEXT))
                .append(CC.format(serverInfo.getState().getInternalName()))
                .build());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(StaffChannel.PERMISSION) && AltaraSettings.STAFF_MESSAGES.get(player)) {
                player.sendMessage(message);
            }
        }
    }
}
