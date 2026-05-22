package games.sparking.altara;

import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AltaraLobbyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ConfigurationService configurationService = new JsonConfigurationService();
        MainConfig mainConfig = configurationService.loadConfiguration(MainConfig.class, new File(getDataFolder(), "config" +
                ".json"));
        new AltaraLobby(this, configurationService, mainConfig);
    }
}
