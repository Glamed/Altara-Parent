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

/** A grant change queued while the API was unreachable; reports the outcome to its issuer. */
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
        String name = UUIDCache.getName(uuid);
        String target = name != null ? name : uuid.toString();
        String action = grant.isRemoved() ? "removal" : "grant";

        Component message = response.wasSuccessful()
                ? CC.success("Queued " + action + " applied.", "The " + action + " for *" + target + "* went through.")
                : CC.error("Queued " + action + " failed.", "*" + target + "*: " + response.getErrorMessage()
                        + " (" + response.getCode() + ")");

        if (UUIDUtils.isUUID(grant.getGrantedBy()))
            new PlayerMessagePacket(UUID.fromString(grant.getGrantedBy()), message).publish();
        else Bukkit.getConsoleSender().sendMessage(message);
    }
}
