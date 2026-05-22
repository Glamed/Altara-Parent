package games.sparking.altara.command.playersetting;

import java.util.List;

public interface PlayerSettingProvider {

    List<PlayerSetting> getProvidedSettings();

    int getPriority();

}
