package games.sparking.altara;

import com.github.retrooper.packetevents.PacketEvents;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.LobbyConfig;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AltaraLobbyPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        ConfigurationService configurationService = new JsonConfigurationService();
        LobbyConfig localConfig = configurationService.loadConfiguration(LobbyConfig.class, new File(getDataFolder(), "config" +
                ".json"));
        new AltaraLobby(this, configurationService, localConfig);
    }
}
