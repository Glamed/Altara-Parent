package games.sparking.altara.logging;

import games.sparking.altara.AltaraPaper;

public class PaperLogger implements CommonLogger {

    @Override
    public void info(String msg) {
        AltaraPaper.getPaperInstance().getLogger().info(msg);
    }

    @Override
    public void warn(String msg) {
        AltaraPaper.getPaperInstance().getLogger().warning(msg);
    }

    @Override
    public void error(String msg) {
        AltaraPaper.getPaperInstance().getLogger().severe(msg);
    }
}