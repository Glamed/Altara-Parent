package games.sparking.altara.playersetting;

import java.util.List;

public interface PlayerSettingProvider {

    List<PlayerSetting> getProvidedSettings();

    int getPriority();

}
