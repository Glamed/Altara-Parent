package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import games.sparking.altara.framework.GameManager;
import games.sparking.altara.framework.GameScanner;
import games.sparking.altara.games.duels.DuelGame;
import games.sparking.altara.games.duels.command.DuelCommand;
import games.sparking.altara.games.duels.module.DuelCombatModule;
import games.sparking.altara.games.duels.module.DuelLifecycleModule;
import games.sparking.altara.games.duels.module.DuelScoreboardModule;
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
        CommandService.register(AltaraPaper.getPlugin(),
                new DuelCommand()
        );
    }

    @Override
    public void registerListeners() {
        super.registerListeners();
    }

    /**
     * Bootstraps the game framework:
     * <ol>
     *   <li>Creates the {@link GameManager} singleton</li>
     *   <li>Instantiates and registers each {@link games.sparking.altara.framework.Game}</li>
     *   <li>Scans each module with {@link GameScanner} — compiles @GameEvent methods
     *       into MethodHandle-backed lambdas and wires them to the {@link games.sparking.altara.framework.EventBus}</li>
     * </ol>
     *
     * <p>After this method returns, the entire event pipeline runs without any
     * reflection — pure Java loops and MethodHandle invokes.
     */
    private void bootGames() {
        gameManager = new GameManager();

        // ── Register games ────────────────────────────────────────────────────
        DuelGame duelGame = new DuelGame();
        gameManager.register(duelGame);

        // ── Scan modules (one-time reflection → compiled MethodHandles) ───────
        JavaPlugin plugin = AltaraPaper.getPlugin();

        GameScanner.scan(new DuelCombatModule(),    gameManager, plugin);
        GameScanner.scan(new DuelLifecycleModule(), gameManager, plugin);
        GameScanner.scan(new DuelScoreboardModule(), gameManager, plugin);

        plugin.getLogger().info("[GameFramework] Booted "
                + gameManager.getGames().size() + " game(s).");
    }
}
