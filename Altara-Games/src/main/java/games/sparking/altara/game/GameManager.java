package games.sparking.altara.game;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.impl.Game;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Central manager for the Altara game framework.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><b>Game-type registry</b> – maps string IDs to {@link Game} factories so that any
 *       number of different game types can co-exist on the same server.</li>
 *   <li><b>Active-instance tracking</b> – multiple {@link Game} instances of any registered
 *       type can run simultaneously.  Each instance is keyed by its unique {@link UUID}.</li>
 *   <li><b>Player routing</b> – keeps a fast look-up from player UUID to the game they are
 *       currently participating in, enforcing the one-game-at-a-time rule.</li>
 *   <li><b>Disconnect handling</b> – automatically removes players from their game when they
 *       quit the server.</li>
 * </ul>
 *
 * <h2>Initialisation</h2>
 * Call {@link #init()} exactly once during plugin startup (e.g. inside
 * {@link games.sparking.altara.AltaraGames#AltaraGames}).
 * <pre>{@code
 * GameManager manager = GameManager.init();
 * manager.registerGameType("skywars", SkyWars::new);
 * manager.registerGameType("bedwars", BedWars::new);
 * }</pre>
 */
public class GameManager implements Listener {

    @Getter private static GameManager instance;

    // -------------------------------------------------------------------------
    // Registry: typeId -> factory
    // -------------------------------------------------------------------------

    private final Map<String, Supplier<Game>> typeRegistry = new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Active instances: instanceId -> Game
    // -------------------------------------------------------------------------

    @Getter private final Map<UUID, Game> activeGames = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Player routing: playerUUID -> instanceId
    // -------------------------------------------------------------------------

    private final Map<UUID, UUID> playerGameMap = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Constructor / static factory
    // -------------------------------------------------------------------------

    private GameManager() {}

    /**
     * Initialises and returns the singleton {@link GameManager}.
     * Also registers it as a Bukkit event listener.
     *
     * @return the newly created {@link GameManager}
     * @throws IllegalStateException if called more than once
     */
    public static GameManager init() {
        if (instance != null) {
            throw new IllegalStateException("GameManager has already been initialised.");
        }
        instance = new GameManager();
        Bukkit.getPluginManager().registerEvents(instance, AltaraPaper.getPlugin());
        return instance;
    }

    // =========================================================================
    // Game-type registry
    // =========================================================================

    /**
     * Registers a new game type with a string ID and a no-arg factory.
     *
     * <p>The ID is case-insensitive. Registering the same ID again will
     * overwrite the previous factory.
     *
     * @param typeId  the unique identifier for this game type (e.g. {@code "skywars"})
     * @param factory a {@link Supplier} that produces a fresh {@link Game} instance
     */
    public void registerGameType(String typeId, Supplier<Game> factory) {
        typeRegistry.put(typeId.toLowerCase(), factory);
        AltaraPaper.getPlugin().getLogger()
                .info("[GameManager] Registered game type: " + typeId.toLowerCase());
    }

    /**
     * Removes a game type from the registry.
     * Already-running instances of that type are not affected.
     *
     * @param typeId the type ID to remove
     */
    public void unregisterGameType(String typeId) {
        typeRegistry.remove(typeId.toLowerCase());
    }

    /** @return an unmodifiable view of all registered type IDs. */
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(typeRegistry.keySet());
    }

    /** @return {@code true} if a game type with the given ID is registered. */
    public boolean isTypeRegistered(String typeId) {
        return typeRegistry.containsKey(typeId.toLowerCase());
    }

    // =========================================================================
    // Game lifecycle
    // =========================================================================

    /**
     * Creates a new game instance of the given type and immediately advances it
     * through {@link GameState#Loading} → {@link GameState#Recruit} so players
     * can start joining.
     *
     * @param typeId the registered game type ID
     * @return an {@link Optional} containing the new {@link Game}, or empty if
     *         the type is not registered
     */
    public Optional<Game> createGame(String typeId) {
        Supplier<Game> factory = typeRegistry.get(typeId.toLowerCase());
        if (factory == null) return Optional.empty();

        Game game = factory.get();
        activeGames.put(game.getInstanceId(), game);

        // Advance to Loading — the game's onLoad() is responsible for calling
        // setState(Recruit) when it is ready (immediately for map-less games,
        // or asynchronously after MapLoader completes for map-based games).
        game.setState(GameState.Loading);
        return Optional.of(game);
    }

    /**
     * Looks up an active game instance by its full {@link UUID}.
     *
     * @param instanceId the instance UUID
     * @return an {@link Optional} containing the game, or empty if not found
     */
    public Optional<Game> getGame(UUID instanceId) {
        return Optional.ofNullable(activeGames.get(instanceId));
    }

    /**
     * Looks up an active game instance by its short ID (case-insensitive).
     *
     * @param shortId the 8-character short ID
     * @return an {@link Optional} containing the first match, or empty
     */
    public Optional<Game> getGameByShortId(String shortId) {
        return activeGames.values().stream()
                .filter(g -> g.getShortId().equalsIgnoreCase(shortId))
                .findFirst();
    }

    /**
     * Returns all active game instances of the given type.
     *
     * @param typeId the game type ID
     * @return a list of matching games (may be empty)
     */
    public List<Game> getActiveGamesByType(String typeId) {
        // We identify the type by looking at the game's getName() – or you can tag
        // each game with a typeId field.  Since each Game subclass provides getName()
        // we simply filter by the factory's key.  Because instances don't store their
        // type ID natively, we resolve it via the registry.
        return activeGames.values().stream()
                .filter(g -> resolveTypeId(g).equalsIgnoreCase(typeId))
                .collect(Collectors.toList());
    }

    /**
     * Destroys an active game, removing it from tracking.
     * This is called automatically by {@link Game#setState(GameState)} when the
     * state reaches {@link GameState#Dead}.  You should not normally call this directly.
     *
     * @param instanceId the ID of the instance to remove
     */
    public void destroyGame(UUID instanceId) {
        Game game = activeGames.remove(instanceId);
        if (game != null) {
            // Clean up player-game mappings for any remaining players
            game.getPlayers().keySet().forEach(playerGameMap::remove);
            AltaraPaper.getPlugin().getLogger()
                    .info("[GameManager] Game destroyed: " + game);
        }
    }

    // =========================================================================
    // Player routing
    // =========================================================================

    /**
     * Records that a player has joined a game instance.
     * Called internally by {@link Game#addPlayer(Player)}.
     */
    public void registerPlayerGame(Player player, Game game) {
        playerGameMap.put(player.getUniqueId(), game.getInstanceId());
    }

    /**
     * Removes the player-game mapping.
     * Called internally by {@link Game#removePlayer(Player)}.
     */
    public void unregisterPlayerGame(Player player) {
        playerGameMap.remove(player.getUniqueId());
    }

    /**
     * Returns the active {@link Game} that the given player is currently in,
     * or {@code null} if the player is not in any game.
     *
     * @param player the player to look up
     * @return the player's current game, or {@code null}
     */
    public Game getPlayerGame(Player player) {
        UUID gameId = playerGameMap.get(player.getUniqueId());
        if (gameId == null) return null;
        return activeGames.get(gameId);
    }

    // =========================================================================
    // Bukkit event listener
    // =========================================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Game game = getPlayerGame(event.getPlayer());
        if (game != null) {
            game.removePlayer(event.getPlayer());
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /**
     * Infers the type ID of a running game by matching its factory against the registry.
     * Falls back to the game's class simple name if no match is found.
     */
    private String resolveTypeId(Game game) {
        for (Map.Entry<String, Supplier<Game>> entry : typeRegistry.entrySet()) {
            Game sample = entry.getValue().get();
            if (sample.getClass() == game.getClass()) return entry.getKey();
        }
        return game.getClass().getSimpleName().toLowerCase();
    }
}

