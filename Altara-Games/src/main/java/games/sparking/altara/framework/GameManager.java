package games.sparking.altara.framework;

import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central registry for all games and the player-to-game mapping.
 *
 * <p>All lookups are O(1) HashMap operations — the hot path during gameplay
 * never touches reflection or annotation metadata.
 */
public class GameManager {

    @Getter
    private static GameManager instance;

    /** id → Game */
    private final Map<String, Game> games = new HashMap<>();

    /** player UUID → active Game */
    private final Map<UUID, Game> playerGameMap = new HashMap<>();

    public GameManager() {
        instance = this;
    }

    // -------------------------------------------------------------------------
    // Game registration
    // -------------------------------------------------------------------------

    public void register(Game game) {
        games.put(game.id(), game);
        game.start();
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

    /** Gracefully stop all games (called on plugin disable). */
    public void shutdown() {
        games.values().forEach(Game::stop);
        playerGameMap.clear();
        games.clear();
    }
}

