package games.sparking.altara.teleport;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportService {

    private static final long PENDING_TIMEOUT = 10_000L;

    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    public void prepare(UUID playerUuid, UUID targetUuid, String targetName, boolean vanish) {
        pendingTeleports.put(playerUuid, new PendingTeleport(
                playerUuid,
                targetUuid,
                targetName,
                vanish,
                System.currentTimeMillis()
        ));
    }

    public void send(Player player, UUID targetUuid, String targetName, String server, boolean vanish) {
        Player target = Bukkit.getPlayer(targetUuid);

        if (target != null) {
            player.teleportAsync(target.getLocation()).thenAccept(success -> {
                if (!success) {
                    player.sendMessage(CC.error(
                            "Teleport failed.",
                            "Unable to teleport to " + target.getName() + "."
                    ));
                    return;
                }

                player.sendMessage(CC.success(
                        "Teleported to *" + target.getName() + "*."
                ));

                if (!vanish) {
                    target.sendMessage(CC.info(
                            "*" + player.getName() + "* teleported to you."
                    ));
                }
            });

            return;
        }

        player.sendMessage(CC.info("Sending you to *" + server + "*..."));

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        player.sendPluginMessage(
                AltaraPaper.getPlugin(),
                "BungeeCord",
                out.toByteArray()
        );
    }

    public PendingTeleport consume(UUID playerUuid) {
        PendingTeleport pending = pendingTeleports.remove(playerUuid);
        if (pending == null) return null;

        if (System.currentTimeMillis() - pending.getCreatedAt() > PENDING_TIMEOUT) {
            return null;
        }

        return pending;
    }

    public PendingTeleport get(UUID playerUuid) {
        PendingTeleport pending = pendingTeleports.get(playerUuid);
        if (pending == null) return null;

        if (System.currentTimeMillis() - pending.getCreatedAt() > PENDING_TIMEOUT) {
            pendingTeleports.remove(playerUuid, pending);
            return null;
        }

        return pending;
    }

    @Getter
    public static class PendingTeleport {

        private final UUID playerUuid;
        private final UUID targetUuid;
        private final String targetName;
        private final boolean vanish;
        private final long createdAt;

        public PendingTeleport(
                UUID playerUuid,
                UUID targetUuid,
                String targetName,
                boolean vanish,
                long createdAt
        ) {
            this.playerUuid = playerUuid;
            this.targetUuid = targetUuid;
            this.targetName = targetName;
            this.vanish = vanish;
            this.createdAt = createdAt;
        }
    }
}