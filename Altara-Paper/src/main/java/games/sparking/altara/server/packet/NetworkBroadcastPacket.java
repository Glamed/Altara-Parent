package games.sparking.altara.server.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.redis.packet.Packet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class NetworkBroadcastPacket extends Packet {
    // Stored as a MiniMessage string for GSON/Redis serialization
    private final String message;
    private final String permission;
    private final boolean staff;
    private final String targetServer;

    public NetworkBroadcastPacket(Component message) {
        this(message, null, false, null);
    }

    public NetworkBroadcastPacket(Component message, String permission) {
        this(message, permission, false, null);
    }

    public NetworkBroadcastPacket(Component message, String permission, boolean staff) {
        this(message, permission, staff, null);
    }

    public NetworkBroadcastPacket(Component message, String permission, String targetServer) {
        this(message, permission, false, targetServer);
    }

    public NetworkBroadcastPacket(Component message, String permission, boolean staff, String targetServer) {
        this.message = MiniMessage.miniMessage().serialize(message);
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

        Component component = MiniMessage.miniMessage().deserialize(message);

        Bukkit.getConsoleSender().sendMessage(component);

        Bukkit.getOnlinePlayers().forEach(player -> {

            if (permission != null && !player.hasPermission(permission)) {
                return;
            }

            if (staff) {
                if (AltaraSettings.STAFF_MESSAGES.get(player)) {
                    player.sendMessage(component);
                }
            } else {
                player.sendMessage(component);
            }
        });
    }
}