package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.game.GameIdRef;
import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.GameTypeRef;
import games.sparking.altara.game.command.GameCommand;
import games.sparking.altara.game.command.parameter.GameIdParameter;
import games.sparking.altara.game.command.parameter.GameTypeParameter;
import games.sparking.altara.game.games.bomblobbers.BombLobbers;
import games.sparking.altara.game.games.micro.MicroGame;
import games.sparking.altara.game.games.skywars.SkyWars;
import games.sparking.altara.game.games.skywars.TeamSkyWars;
import games.sparking.altara.game.spectator.SpectatorListener;
import games.sparking.altara.game.spectator.SpectatorVisibilityAdapter;
import games.sparking.altara.visibility.VisibilityService;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraGames extends AltaraPaper {

    public AltaraGames(JavaPlugin instance) {
        super(instance);
        Altara.setServerIdentifier("Games");

        // Initialise the game framework (registry + player routing + disconnect handler)
        GameManager manager = GameManager.init();

        // ── Register game-specific parameter types ────────────────────────
        CommandService.registerParameter(GameTypeRef.class, new GameTypeParameter());
        CommandService.registerParameter(GameIdRef.class, new GameIdParameter());

        // ── Register game types ───────────────────────────────────────────
        manager.registerGameType("bomblobbers",   BombLobbers::new);
        manager.registerGameType("micro",         MicroGame::new);
        manager.registerGameType("skywars",       SkyWars::new);
        manager.registerGameType("skywars-teams", TeamSkyWars::new);

        registerCommands();
        registerListener();
        getPaperInstance().getServer().getConsoleSender().sendMessage(Component.text("Altara Games has booted"));
    }

    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPaperInstance(),
                new GameCommand()
        );
    }

    @Override
    public void registerListener() {
        super.registerListener();
        // Spectating: visibility adapter (hides spectators from alive players)
        VisibilityService.init();
        VisibilityService.registerVisibilityAdapter(new SpectatorVisibilityAdapter());
        // Spectating: global event handler (compass cycling, damage/pickup guards)
        getPaperInstance().getServer().getPluginManager()
                .registerEvents(new SpectatorListener(), getPaperInstance());
    }
}
