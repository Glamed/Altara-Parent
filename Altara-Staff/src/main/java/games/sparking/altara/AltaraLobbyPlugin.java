package games.sparking.altara;

import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.StaffConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AltaraLobbyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ConfigurationService configurationService = new JsonConfigurationService();
        StaffConfig localConfig = configurationService.loadConfiguration(StaffConfig.class, new File(getDataFolder(), "config" +
                ".json"));
        new AltaraLobby(this, configurationService, localConfig);
    }

}
