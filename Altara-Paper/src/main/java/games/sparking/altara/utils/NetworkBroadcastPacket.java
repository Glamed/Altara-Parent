package games.sparking.altara.utils;

import games.sparking.altara.redis.packet.Packet;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.maven.model.Profile;
import org.bukkit.Bukkit;

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
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Bukkit.getConsoleSender().sendMessage(miniMessage.deserialize(this.message));
    }

}
