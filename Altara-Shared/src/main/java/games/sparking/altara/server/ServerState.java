package games.sparking.altara.server;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServerState {

    ONLINE("Online"),
    OFFLINE("Offline", "Offline (Plugin Disabled)"),
    WHITELISTED("Whitelisted"),
    HEARTBEAT_TIMEOUT("Offline", "Offline (Heartbeat Timeout)"),
    UNKNOWN("Offline", "Offline (Unknown)");

    private final String displayName;
    private final String internalName;

    ServerState(String displayName) {
        this(displayName, displayName);
    }

}