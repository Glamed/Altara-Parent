package games.sparking.altara.grant;

import games.sparking.altara.connection.BackLogEntry;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PlayerMessagePacket;
import games.sparking.altara.uuid.UUIDCache;
import net.kyori.adventure.text.Component;
import okhttp3.Request;
import org.bukkit.Bukkit;

import java.util.UUID;

/** A grant clear queued while the API was unreachable; reports the outcome to its issuer. */
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
        String name = UUIDCache.getName(uuid);
        String target = name != null ? name : uuid.toString();

        Component message;
        if (response.wasSuccessful()) {
            int removed = response.asObject().get("removed").getAsInt();
            message = CC.success("Queued clear applied.", "Removed *" + removed + "* " + CC.plural(removed, "grant")
                    + " from *" + target + "*.");
        } else {
            message = CC.error("Queued clear failed.", "*" + target + "*: " + response.getErrorMessage()
                    + " (" + response.getCode() + ")");
        }

        if (clearedBy == null) Bukkit.getConsoleSender().sendMessage(message);
        else new PlayerMessagePacket(clearedBy, message).publish();
    }
}
