package games.sparking.altara.report;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.presence.PresenceService;
import games.sparking.altara.staffmode.StaffMode;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-network "follow the suspect" mechanic — mirrors {@code games.sparking.altara.teleport}
 * exactly: if the suspect isn't here, send the handler across the proxy to the server that
 * has them and finish the teleport once they join there.
 */
public class ReportFollowService {

    private static final long PENDING_TIMEOUT = 15_000L;

    private final Map<UUID, PendingFollow> pendingFollows = new ConcurrentHashMap<>();

    /**
     * Main thread.  Sends the handler to the suspect: a direct teleport if they're here,
     * otherwise across the proxy to {@code suspectServer} (looked up off-thread by the
     * caller via {@link PresenceService#getServer}; {@code null} means offline).
     */
    public void followSuspect(Player handler, UUID suspectUuid, String suspectName, String suspectServer) {
        Player localSuspect = Bukkit.getPlayer(suspectUuid);
        if (localSuspect != null) {
            teleportAndSpectate(handler, localSuspect);
            return;
        }

        String server = suspectServer;
        if (server == null) {
            handler.sendMessage(CC.notice("*" + suspectName + "* doesn't appear to be online.",
                    "You'll be brought to them automatically if they reconnect."));
            return;
        }

        sendToServer(handler, suspectUuid, suspectName, server);
    }

    /** Sends {@code handler} across the proxy to {@code server}, where {@code suspectUuid} currently is. */
    public void sendToServer(Player handler, UUID suspectUuid, String suspectName, String server) {
        if (server.equalsIgnoreCase(AltaraPaper.getPaperInstance().getLocalServerName())) {
            // We were told to send them here — the suspect should already be local by now.
            Player localSuspect = Bukkit.getPlayer(suspectUuid);
            if (localSuspect != null) teleportAndSpectate(handler, localSuspect);
            return;
        }

        pendingFollows.put(handler.getUniqueId(), new PendingFollow(suspectUuid, suspectName, System.currentTimeMillis()));

        handler.sendMessage(CC.notice("Following *" + suspectName + "*.", "Sending you to *" + server + "*..."));

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        handler.sendPluginMessage(AltaraPaper.getPlugin(), "BungeeCord", out.toByteArray());
    }

    public PendingFollow consume(UUID handlerUuid) {
        PendingFollow pending = pendingFollows.remove(handlerUuid);
        if (pending == null) return null;
        if (System.currentTimeMillis() - pending.getCreatedAt() > PENDING_TIMEOUT) return null;
        return pending;
    }

    public PendingFollow get(UUID handlerUuid) {
        PendingFollow pending = pendingFollows.get(handlerUuid);
        if (pending == null) return null;
        if (System.currentTimeMillis() - pending.getCreatedAt() > PENDING_TIMEOUT) {
            pendingFollows.remove(handlerUuid, pending);
            return null;
        }
        return pending;
    }

    public void teleportAndSpectate(Player handler, Player suspect) {
        StaffMode.ensureSpectating(handler);
        handler.teleportAsync(suspect.getLocation()).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                handler.sendMessage(CC.success("Following *" + suspect.getName() + "*."));
            }
        });
    }

    @Getter
    public static class PendingFollow {

        private final UUID suspectUuid;
        private final String suspectName;
        private final long createdAt;

        public PendingFollow(UUID suspectUuid, String suspectName, long createdAt) {
            this.suspectUuid = suspectUuid;
            this.suspectName = suspectName;
            this.createdAt = createdAt;
        }
    }
}
