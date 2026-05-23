package games.sparking.altara.logging;

import games.sparking.altara.AltaraPaper;

public class PaperLogger implements CommonLogger {

    @Override
    public void info(String msg) {
        AltaraPaper.getPlugin().getLogger().info(msg);
    }

    @Override
    public void warn(String msg) {
        AltaraPaper.getPlugin().getLogger().warning(msg);
    }

    @Override
    public void error(String msg) {
        AltaraPaper.getPlugin().getLogger().severe(msg);
    }
}