package games.sparking.altara.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a module belonging to a specific game.
 * Modules contain @GameEvent handler methods that are scanned at startup
 * and compiled into fast lookup handlers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterModule {
    /** The game id this module belongs to (must match @RegisterGame id). */
    String game();
}

