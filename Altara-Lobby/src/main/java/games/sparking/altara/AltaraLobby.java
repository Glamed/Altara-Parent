package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import games.sparking.altara.configuration.defaults.MainConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraLobby extends AltaraPaper {

    public AltaraLobby(JavaPlugin instance, ConfigurationService configurationService, LocalConfig localConfig) {
        super(instance, configurationService, localConfig);
        Altara.setServerIdentifier("Lobby");
        registerCommands();
        registerListeners();
        getPlugin().getServer().getConsoleSender().sendMessage(Component.text("Altara lobby has booted"));
    }


    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPlugin(),
                new AltaraCommand()
        );
    }
}
