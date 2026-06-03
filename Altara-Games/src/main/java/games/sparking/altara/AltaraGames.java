package games.sparking.altara;

import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import games.sparking.altara.framework.GameManager;
import games.sparking.altara.games.skywars.SkyWarsDuosGame;
import games.sparking.altara.games.skywars.SkyWarsSolosGame;
import games.sparking.altara.games.survivalgames.SurvivalGamesDuosGame;
import games.sparking.altara.games.survivalgames.SurvivalGamesSolosGame;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraGames extends AltaraPaper {

    @Getter
    private GameManager gameManager;

    public AltaraGames(JavaPlugin instance, ConfigurationService configurationService, LocalConfig localConfig) {
        super(instance, configurationService, localConfig);

        registerCommands();
        registerListeners();
        bootGames();
        getPlugin().getServer().getConsoleSender().sendMessage(Component.text("Altara Games has booted"));
    }

    @Override
    public void registerCommands() {
        super.registerCommands();
    }

    @Override
    public void registerListeners() {
        super.registerListeners();
    }

    /**
     * Bootstraps the game framework:
     * <ol>
     *   <li>Creates the {@link GameManager} singleton (receives the plugin reference
     *       so it can register Bukkit listeners internally).</li>
     *   <li>Registers each game — the manager automatically calls
     *       {@link games.sparking.altara.framework.Game#modules()}, sets up every module,
     *       compiles all {@code @GameEvent} methods into MethodHandle lambdas, and
     *       wires them to the {@link games.sparking.altara.framework.EventBus}.</li>
     * </ol>
     *
     * <p>Adding a new game = one line: {@code gameManager.register(new MyGame())}.
     * No module scanning, no annotation discovery, no changes to this class.
     */
    private void bootGames() {
        gameManager = new GameManager(AltaraPaper.getPlugin());

        // SkyWars
        gameManager.register(new SkyWarsSolosGame());
        gameManager.register(new SkyWarsDuosGame());

        // Survival Games
        gameManager.register(new SurvivalGamesSolosGame());
        gameManager.register(new SurvivalGamesDuosGame());
    }
}
