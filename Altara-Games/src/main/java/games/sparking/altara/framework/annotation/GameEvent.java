package games.sparking.altara.framework.annotation;

import games.sparking.altara.framework.GameState;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a @RegisterModule class as a game event handler.
 *
 * <p>Method signature must be:
 * <pre>
 *   void methodName(SomeEvent event, Game game, Player player, GameState state)
 * </pre>
 *
 * <p>The framework will:
 * <ol>
 *   <li>Extract the relevant player from the event (via {@link games.sparking.altara.framework.PlayerExtractor})</li>
 *   <li>Look up the player's active game via {@link games.sparking.altara.framework.GameManager}</li>
 *   <li>Filter by the specified game states</li>
 *   <li>Invoke the method via a pre-compiled MethodHandle — zero reflection at runtime</li>
 * </ol>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameEvent {

    /** The Bukkit event type this handler listens for. */
    Class<? extends Event> value();

    /** Which player states should trigger this handler. Defaults to PLAYING. */
    GameState[] states() default {GameState.PLAYING};

    /** Bukkit event priority for the underlying listener registration. */
    EventPriority priority() default EventPriority.NORMAL;

    /** Whether to skip events that have been cancelled by other listeners. */
    boolean ignoreCancelled() default false;
}

