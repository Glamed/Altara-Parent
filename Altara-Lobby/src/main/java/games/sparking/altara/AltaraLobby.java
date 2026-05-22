package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraLobby extends AltaraPaper {

    public AltaraLobby(JavaPlugin instance, ConfigurationService configurationService, MainConfig mainConfig) {
        super(instance, configurationService, mainConfig);
        Altara.setServerIdentifier("Lobby");
        registerCommands();
        registerListeners();
        getPaperInstance().getServer().getConsoleSender().sendMessage(Component.text("Altara lobby has booted"));
    }


    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPaperInstance(),
                new AltaraCommand()
        );
    }
}
