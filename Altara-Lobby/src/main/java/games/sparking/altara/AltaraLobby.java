package games.sparking.altara;

import games.sparking.altara.chat.ChatListener;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LobbyConfig;
import games.sparking.altara.hologram.listener.HologramListener;
import games.sparking.altara.leaderboards.StaffHologramManager;
import games.sparking.altara.menu.listener.MenuListener;
import games.sparking.altara.profiler.ProfilerListener;
import games.sparking.altara.punishment.listener.PunishmentChatListener;
import games.sparking.altara.punishment.listener.PunishmentLoginListener;
import games.sparking.altara.scoreboard.HubBoardAdapter;
import games.sparking.altara.scoreboard.ScoreboardListener;
import games.sparking.altara.scoreboard.ScoreboardService;
import games.sparking.altara.spawn.SpawnCommands;
import games.sparking.altara.updater.FileUpdater;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

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

    @Override
    public void registerListeners() {
        super.registerListeners();
        Arrays.asList(
                new StaffHologramManager()
        ).forEach(listener -> getPlugin().getServer().getPluginManager().registerEvents(listener, getPlugin()));

    }

}
