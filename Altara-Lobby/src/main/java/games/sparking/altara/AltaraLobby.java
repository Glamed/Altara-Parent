package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LobbyConfig;
import games.sparking.altara.leaderboards.StaffHologramManager;
import games.sparking.altara.npc.NpcManager;
import games.sparking.altara.playersetting.LobbySettings;
import games.sparking.altara.playersetting.PlayerSettingService;
import games.sparking.altara.scoreboard.HubBoardAdapter;
import games.sparking.altara.scoreboard.ScoreboardService;
import games.sparking.altara.spawn.SpawnCommands;
import games.sparking.altara.spawn.SpawnListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class AltaraLobby extends AltaraPaper {

    @Getter
    private static AltaraLobby lobbyInstance;
    @Getter private LobbyConfig LobbyConfig;
    @Getter private NpcManager npcManager;

    public AltaraLobby(JavaPlugin instance, ConfigurationService configurationService, LobbyConfig localConfig) {
        super(instance, configurationService, localConfig);
        lobbyInstance = this;
        this.LobbyConfig = localConfig;
        new ScoreboardService(new HubBoardAdapter());

        PlayerSettingService.registerProvider(new LobbySettings());

        registerCommands();
        registerListeners();

        // Load and spawn all server-selector NPCs with their live-data holograms.
        this.npcManager = new NpcManager(this);
        npcManager.loadNpcs();
    }

    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPlugin(),
                new SpawnCommands()
        );
    }

    @Override
    public void registerListeners() {
        super.registerListeners();
        List.of(
                new StaffHologramManager(),
                new SpawnListener()
        ).forEach(listener -> getPlugin().getServer().getPluginManager().registerEvents(listener, getPlugin()));

    }

}
