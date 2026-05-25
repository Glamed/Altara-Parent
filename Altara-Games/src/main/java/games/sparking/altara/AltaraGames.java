package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraGames extends AltaraPaper {

    public AltaraGames(JavaPlugin instance, ConfigurationService configurationService, LocalConfig localConfig) {
        super(instance, configurationService, localConfig);

        registerCommands();
        registerListeners();
        getPlugin().getServer().getConsoleSender().sendMessage(Component.text("Altara Games has booted"));
    }

    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPlugin(), null
        );
    }

    @Override
    public void registerListeners() {
        super.registerListeners();
    }
}
