package games.sparking.altara.grant;

import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.connection.BackLogEntry;
import games.sparking.blazora.connection.RequestResponse;
import games.sparking.blazora.profile.packets.ProfileUpdatePacket;
import games.sparking.blazora.utils.CC;
import games.sparking.blazora.utils.PlayerMessagePacket;
import games.sparking.blazora.uuid.UUIDCache;
import okhttp3.Request;
import org.bukkit.Bukkit;

import java.util.UUID;

public class GrantClearBackLogEntry extends BackLogEntry {

    private static final BlazoraPaper zircon = BlazoraPaper.getPaperInstance();

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
        else zircon.getRedisService().publish(new PlayerMessagePacket(clearedBy, message));

        if (response.wasSuccessful())
            zircon.getRedisService().publish(new ProfileUpdatePacket(uuid));
    }
}
