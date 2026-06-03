package games.sparking.altara.framework;

import java.util.List;
import java.util.UUID;

/**
 * Core interface every game must implement.
 *
 * <p>A game is the top-level owner of a set of {@link GameModule}s.  When the
 * game is registered with {@link GameManager}, the manager calls
 * {@link #modules()} and automatically wires each module's {@code @GameEvent}
 * methods into the {@link EventBus} — no manual scanning in plugin bootstrap
 * code is ever needed.
 *
 * <p>To add a new game to the server you only need to:
 * <ol>
 *   <li>Create a class that implements {@code Game}.</li>
 *   <li>Override {@link #modules()} to return the modules that belong to it.</li>
 *   <li>Call {@code gameManager.register(new MyGame())} once in your bootstrap.</li>
 * </ol>
 */
public interface Game {

    /** Unique identifier for this game type (e.g. {@code "duels"}, {@code "bedwars"}). */
    String id();

    /**
     * Returns the {@link GameModule}s owned by this game.
     *
     * <p>The {@link GameManager} calls this once during {@link GameManager#register}
     * to compile and wire all {@code @GameEvent} handlers.  The order of the returned
     * list controls the order handlers are invoked for the same event type.
     *
     * <p>Modules should be stored as fields on the game and constructed with
     * {@code this} so they have a typed reference back to their game.
     */
    default List<GameModule> modules() {
        return List.of();
    }

    /** Called once after all modules are set up, when the game is fully registered. */
    void start();

    /** Called once when the plugin disables. Should gracefully end any active sessions. */
    void stop();

    /**
     * Returns the current state of a player in this game.
     * Returns {@link GameState#LOBBY} if the player is not actively tracked.
     */
    GameState getState(UUID player);

    /**
     * Fast active-session check used by the event pipeline before dispatching
     * any handler.  Return {@code true} only when the player is in an ongoing
     * session managed by this game.
     */
    boolean isActive(UUID player);
}

