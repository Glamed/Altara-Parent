package games.sparking.altara.presence;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PlayerPresence {

    private final UUID playerUuid;
    private final String server;
    private final UUID sessionId;
    private final long connectedAt;
    private final long lastHeartbeat;
}