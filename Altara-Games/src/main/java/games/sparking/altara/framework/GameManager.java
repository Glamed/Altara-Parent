package games.sparking.altara.framework;

import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Central registry for all games and the player-to-game mapping.
 *
 * <p>All runtime lookups are O(1) HashMap operations — the hot path during
 * gameplay never touches reflection or annotation metadata.
 *
 * <p><b>Registration flow</b> (called once per game at startup):
 * <ol>
 *   <li>The game's {@link Game#modules()} list is retrieved.</li>
 *   <li>{@link GameModule#setup()} is called on each module.</li>
 *   <li>{@link GameScanner} compiles every {@code @GameEvent} method on each
 *       module into a {@link GameHandler} lambda backed by a {@link java.lang.invoke.MethodHandle}
 *       and wires it to the {@link EventBus} — one-time reflection cost only.</li>
 *   <li>{@link Game#start()} is called on the game itself.</li>
 * </ol>
 *
 * <p>To add a new game you only call {@code gameManager.register(new MyGame())}
 * — no changes to any bootstrap class are needed.
 */
public class GameManager {

    @Getter
    private static GameManager instance;

    private final Plugin plugin;

    /** id → Game */
    private final Map<String, Game> games = new HashMap<>();

    /** game id → modules belonging to that game (kept for cleanup) */
    private final Map<String, List<GameModule>> moduleRegistry = new HashMap<>();

    /** player UUID → active Game */
    private final Map<UUID, Game> playerGameMap = new HashMap<>();

    public GameManager(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    // -------------------------------------------------------------------------
    // Game registration
    // -------------------------------------------------------------------------

    /**
     * Registers a game and automatically sets up all of its modules.
     *
     * <p>The game provides its own modules via {@link Game#modules()} — the
     * manager never needs to know about specific module classes.
     *
     * @param game the game to register
     */
    public void register(Game game) {
        games.put(game.id(), game);

        // Set up and compile every module the game declares
        List<GameModule> modules = game.modules();
        moduleRegistry.put(game.id(), modules);

        for (GameModule module : modules) {
            module.setup();
            GameScanner.scan(module, this, plugin);
        }

        game.start();

        plugin.getLogger().info("[GameManager] Registered game '" + game.id()
                + "' with " + modules.size() + " module(s).");
    }

    public Game getGame(String id) {
        return games.get(id);
    }

    public Collection<Game> getGames() {
        return games.values();
    }

    // -------------------------------------------------------------------------
    // Player ↔ Game mapping
    // -------------------------------------------------------------------------

    /** Called when a player enters an active session. */
    public void setPlayerGame(UUID player, Game game) {
        playerGameMap.put(player, game);
    }

    /**
     * Returns the game the player is currently playing, or {@code null} if the
     * player is not in any active session.
     */
    public Game getGame(UUID player) {
        return playerGameMap.get(player);
    }

    /** Called when a player's session ends (win, loss, forfeit, disconnect). */
    public void removePlayer(UUID player) {
        playerGameMap.remove(player);
    }

    public boolean isPlaying(UUID player) {
        return playerGameMap.containsKey(player);
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    /** Gracefully stops all games and cleans up all modules (called on plugin disable). */
    public void shutdown() {
        // Stop games first so they can end active sessions and message players
        games.values().forEach(Game::stop);

        // Then let each module release its resources
        moduleRegistry.values().forEach(modules ->
                modules.forEach(GameModule::cleanup));

        playerGameMap.clear();
        moduleRegistry.clear();
        games.clear();
    }
}

