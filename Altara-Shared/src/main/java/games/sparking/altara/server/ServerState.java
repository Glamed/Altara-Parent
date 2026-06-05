package games.sparking.altara.server;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServerState {

    ONLINE("<green>Online"),
    OFFLINE("<red>Offline", "<red>Offline <gray>(Plugin Disabled)"),
    WHITELISTED("<yellow>Whitelisted"),
    HEARTBEAT_TIMEOUT("<red>Offline", "<red>Offline <gray>(Heartbeat Timeout)"),
    UNKNOWN("<red>Offline", "<red>Offline <gray>(Unknown)");

    private final String displayName;
    private final String internalName;

    ServerState(String displayName) {
        this(displayName, displayName);
    }

}