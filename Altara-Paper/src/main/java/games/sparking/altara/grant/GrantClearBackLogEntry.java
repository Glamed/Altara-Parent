package games.sparking.altara.grant;

import games.sparking.altara.connection.BackLogEntry;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.profile.packet.ProfileUpdatePacket;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PlayerMessagePacket;
import games.sparking.altara.uuid.UUIDCache;
import okhttp3.Request;
import org.bukkit.Bukkit;

import java.util.UUID;

public class GrantClearBackLogEntry extends BackLogEntry {

    private final UUID uuid;
    private final UUID clearedBy;

    public GrantClearBackLogEntry(UUID uuid, UUID clearedBy, Request.Builder builder) {
        super(builder);
        this.uuid = uuid;
        this.clearedBy = clearedBy;
    }

    @Override
    public void onSend(RequestResponse response) {
        String message;
        if (!response.wasSuccessful())
            message = CC.format("&c[Grant BackLog] Could not clear grants of &e%s&c: %s (%d)",
                    UUIDCache.getName(uuid), response.getErrorMessage(), response.getCode());
        else message = CC.format("&a[Grant BackLog] Successfully cleared &e%d &agrants of &e%s&a.",
                response.asObject().get("removed").getAsInt(), UUIDCache.getName(uuid));

        if (clearedBy == null)
            Bukkit.getConsoleSender().sendMessage(message);
        else new PlayerMessagePacket(clearedBy, message).publish();

        if (response.wasSuccessful())
            new ProfileUpdatePacket(uuid).publish();
    }
}
