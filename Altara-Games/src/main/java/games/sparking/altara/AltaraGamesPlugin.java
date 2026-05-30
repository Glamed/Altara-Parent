package games.sparking.altara;

import com.github.retrooper.packetevents.PacketEvents;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import games.sparking.altara.configuration.defaults.MainConfig;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AltaraGamesPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        ConfigurationService configurationService = new JsonConfigurationService();
        LocalConfig localConfig = configurationService.loadConfiguration(LocalConfig.class, new File(getDataFolder(), "config" +
                ".json"));
        new AltaraGames(this, configurationService, localConfig);
    }
}
