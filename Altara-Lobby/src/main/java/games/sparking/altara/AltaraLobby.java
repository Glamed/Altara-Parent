package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LobbyConfig;
import games.sparking.altara.scoreboard.HubBoardAdapter;
import games.sparking.altara.scoreboard.ScoreboardService;
import games.sparking.altara.spawn.SpawnCommands;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;


public class AltaraLobby extends AltaraPaper {

    @Getter
    private static AltaraLobby lobbyInstance;
    @Getter private LobbyConfig LobbyConfig;

    public AltaraLobby(JavaPlugin instance, ConfigurationService configurationService, LobbyConfig localConfig) {
        super(instance, configurationService, localConfig);
        lobbyInstance = this;
        this.LobbyConfig = localConfig;
        new ScoreboardService(new HubBoardAdapter());

        registerCommands();
        registerListeners();
    }

    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPlugin(),
                new SpawnCommands(),
                new AltaraCommand()
        );
    }

}
