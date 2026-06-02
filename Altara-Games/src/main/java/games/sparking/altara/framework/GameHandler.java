package games.sparking.altara.framework;

import org.bukkit.event.Event;

/**
 * Compiled game handler — a pure Java lambda that is called for every matching
 * Bukkit event.  Reflection only touches this during startup compilation;
 * at runtime it is just a virtual dispatch into a lambda.
 *
 * @param <T> the specific Event subtype this handler handles
 */
@FunctionalInterface
public interface GameHandler<T extends Event> {
    void handle(T event);
}

