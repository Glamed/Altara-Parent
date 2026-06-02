package games.sparking.altara.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a kit belonging to a specific game.
 * Kits are scanned and registered with their game at startup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterKit {
    /** The game id this kit belongs to. */
    String game();
    /** Unique identifier for this kit within the game. */
    String id();
}

