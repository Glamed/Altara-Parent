package games.sparking.altara.configuration;

import games.sparking.altara.configuration.defaults.MainConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class LocalConfig extends MainConfig {

    private long slowChatDelay = -1;

    private boolean chatMuted = false;
    private boolean staffModeOnJoin = true;
    private boolean staffVisible = true;
    private boolean antiVPN = true;

    private boolean queueEnabled = false;
    private boolean queuePaused = false;
    private int queueRate = 2;

}