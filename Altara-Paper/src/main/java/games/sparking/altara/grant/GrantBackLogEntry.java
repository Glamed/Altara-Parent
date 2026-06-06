package games.sparking.altara.grant;

import games.sparking.altara.connection.BackLogEntry;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PlayerMessagePacket;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import okhttp3.Request;
import org.bukkit.Bukkit;

import java.util.UUID;

public class GrantBackLogEntry extends BackLogEntry {

    private final Grant grant;
    private final UUID uuid;

    public GrantBackLogEntry(Grant grant, UUID uuid, Request.Builder builder) {
        super(builder);
        this.grant = grant;
        this.uuid = uuid;
    }

    @Override
    public void onSend(RequestResponse response) {
        Component message;

        if (!response.wasSuccessful())
            message = CC.format(
                    "<red>[Grant BackLog] Could not %s grant for <white>%s</white>: %s (%d)</red>",
                    grant.isRemoved() ? "remove" : "create",
                    UUIDCache.getName(uuid),
                    response.getErrorMessage(),
                    response.getCode()
            );
        else message = CC.format(
                "<green>[Grant BackLog] Successfully %s grant for <white>%s</white>.</green>",
                grant.isRemoved() ? "removed" : "created",
                UUIDCache.getName(uuid)
        );

        if (UUIDUtils.isUUID(grant.getGrantedBy()))
            new PlayerMessagePacket(
                    UUID.fromString(grant.getGrantedBy()),
                    message
            ).publish();
        else Bukkit.getConsoleSender().sendMessage(message);
    }
}
