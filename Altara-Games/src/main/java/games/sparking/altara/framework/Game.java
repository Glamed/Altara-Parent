package games.sparking.altara.framework;

import java.util.UUID;

/**
 * Core interface every game must implement.
 *
 * <p>Implementations should also be annotated with {@link games.sparking.altara.framework.annotation.RegisterGame}
 * so the {@link GameManager} can discover and auto-register them at startup.
 */
public interface Game {

    /** Unique identifier for this game (matches the @RegisterGame id). */
    String id();

    /** Called once when the plugin enables and the game is registered. */
    void start();

    /** Called once when the plugin disables. Should gracefully end any active sessions. */
    void stop();

    /**
     * Returns the current state of a player in this game.
     * Returns {@link GameState#LOBBY} if the player is not actively tracked.
     */
    GameState getState(UUID player);

    /**
     * Returns true if the player is currently in an active session in this game.
     * This is the fast check the event pipeline calls before dispatching handlers.
     */
    boolean isActive(UUID player);
}

