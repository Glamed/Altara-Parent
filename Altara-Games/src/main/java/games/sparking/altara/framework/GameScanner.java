package games.sparking.altara.framework;

import games.sparking.altara.framework.annotation.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Startup scanner that turns {@link GameEvent}-annotated methods into compiled
 * {@link GameHandler} lambdas backed by {@link MethodHandle}s.
 *
 * <p><b>How it works:</b>
 * <ol>
 *   <li>Reflect over the instance's class <em>once</em> during plugin enable.</li>
 *   <li>For each {@code @GameEvent} method, call {@code setAccessible(true)} and
 *       obtain a {@link MethodHandle} bound to the instance — the handle is then
 *       stored inside a lambda closure.</li>
 *   <li>Register the lambda with {@link EventBus}.  From this point on the
 *       event pipeline is pure Java: map lookup + lambda call + MethodHandle
 *       invoke — no reflection, no annotation reads.</li>
 * </ol>
 *
 * <p><b>Expected method signature:</b>
 * <pre>
 *   void onSomething(SomeEvent event, Game game, Player player, GameState state)
 * </pre>
 */
public final class GameScanner {

    private GameScanner() {}

    /**
     * Scans {@code instance} for {@link GameEvent} methods and registers compiled
     * handlers with the {@link EventBus}.
     *
     * @param instance the module object whose methods will be scanned
     * @param manager  the game manager used for player → game lookup at runtime
     * @param plugin   the owning plugin for Bukkit listener registration
     */
    @SuppressWarnings("unchecked")
    public static void scan(Object instance, GameManager manager, Plugin plugin) {
        Class<?> clazz = instance.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(GameEvent.class)) continue;

            GameEvent annotation = method.getAnnotation(GameEvent.class);
            Class<? extends Event> eventClass = annotation.value();
            GameState[] allowedStates = annotation.states();

            // --- Compile to MethodHandle at startup (one-time reflection cost) ---
            method.setAccessible(true);
            final MethodHandle handle;
            try {
                // privateLookupIn gives us access to the module's private members.
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
                // bindTo eliminates the receiver argument; invoke(event, game, player, state)
                handle = lookup.unreflect(method).bindTo(instance);
            } catch (IllegalAccessException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[GameScanner] Could not create MethodHandle for " + clazz.getSimpleName()
                                + "#" + method.getName() + " — skipping.", e);
                continue;
            }

            // --- Build the zero-reflection runtime handler ---
            GameHandler<Event> handler = event -> {
                try {
                    // 1. Extract the relevant player from the event (cached Function, no instanceof chain)
                    Player player = PlayerExtractor.extract(event);
                    if (player == null) return;

                    // 2. O(1) player → game lookup
                    Game game = manager.getGame(player.getUniqueId());
                    if (game == null) return;

                    // 3. Fast active check
                    if (!game.isActive(player.getUniqueId())) return;

                    // 4. State filter (tight loop, tiny array)
                    GameState state = game.getState(player.getUniqueId());
                    boolean stateMatches = false;
                    for (GameState s : allowedStates) {
                        if (s == state) { stateMatches = true; break; }
                    }
                    if (!stateMatches) return;

                    // 5. MethodHandle invoke — JIT-inlineable, effectively direct call
                    handle.invoke(event, game, player, state);

                } catch (Throwable t) {
                    plugin.getLogger().log(Level.WARNING,
                            "[GameScanner] Exception in handler " + clazz.getSimpleName()
                                    + "#" + method.getName(), t);
                }
            };

            // Safe unchecked cast: handler was built specifically for eventClass,
            // and EventBus stores everything erased at runtime anyway.
            @SuppressWarnings("unchecked")
            Class<Event> rawEventClass = (Class<Event>) eventClass;
            EventBus.register(rawEventClass, handler, plugin, annotation.priority(), annotation.ignoreCancelled());

            plugin.getLogger().info("[GameScanner] Compiled handler: "
                    + clazz.getSimpleName() + "#" + method.getName()
                    + " → " + eventClass.getSimpleName());
        }
    }
}

