package games.sparking.altara.grant;

import games.sparking.altara.connection.BackLogEntry;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.profile.packet.ProfileUpdatePacket;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PlayerMessagePacket;
import games.sparking.altara.uuid.UUIDCache;
import net.kyori.adventure.text.Component;
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
        Component message;
        if (!response.wasSuccessful())
            message = CC.format("<red>[Grant BackLog] Could not clear grants of <white>%s</white>: %s (%d)</red>",
                    UUIDCache.getName(uuid), response.getErrorMessage(), response.getCode());
        else message = CC.format("<green>[Grant BackLog] Successfully cleared <white>%d</white> grants of <white>%s</white>.</green>",
                response.asObject().get("removed").getAsInt(), UUIDCache.getName(uuid));

        if (clearedBy == null)
            Bukkit.getConsoleSender().sendMessage(message);
        else new PlayerMessagePacket(clearedBy, message).publish();

        if (response.wasSuccessful())
            new ProfileUpdatePacket(uuid).publish();
    }
}
