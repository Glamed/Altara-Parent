package games.sparking.altara.server;

import games.sparking.altara.Altara;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.redis.packet.Packet;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;

@RequiredArgsConstructor
public class NetworkBroadcastPacket extends Packet {
    private final String message;
    private final String permission;
    private final boolean staff;
    private final String targetServer;

    public NetworkBroadcastPacket(String message) {
        this(message, null, false, null);
    }

    public NetworkBroadcastPacket(String message, String permission) {
        this(message, permission, false, null);
    }

    public NetworkBroadcastPacket(String message, String permission, boolean staff) {
        this(message, permission, staff, null);
    }

    public NetworkBroadcastPacket(String message, String permission, String targetServer) {
        this(message, permission, false, targetServer);
    }

    public NetworkBroadcastPacket(String message, String permission, boolean staff, String targetServer) {
        this.message = message;
        this.permission = permission;
        this.staff = staff;
        this.targetServer = targetServer;
    }

    @Override
    public void receive() {

        if (targetServer != null) {
            if (!Altara.getSharedInstance().getLocalServerName().equals(targetServer)) {
                return;
            }
        }

        Bukkit.getConsoleSender().sendMessage(message);

        Bukkit.getOnlinePlayers().forEach(player -> {

            if (permission != null && !player.hasPermission(permission)) {
                return;
            }

            if (staff) {
                if (AltaraSettings.STAFF_MESSAGES.get(player)) {
                    player.sendMessage(message);
                }
            } else {
                player.sendMessage(message);
            }
        });
    }
}