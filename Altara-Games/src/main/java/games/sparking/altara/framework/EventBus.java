package games.sparking.altara.framework;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zero-overhead event routing layer.
 *
 * <p>Only <em>one</em> Bukkit listener is ever registered per event type.
 * All {@link GameHandler} lambdas for a given event type share that single
 * registration, so Bukkit's internal per-event overhead stays constant
 * regardless of how many modules subscribe to the same event.
 *
 * <p>Handlers are stored in a {@link CopyOnWriteArrayList} so adding handlers
 * at startup is safe and iterating during gameplay is allocation-free.
 */
public final class EventBus {

    private EventBus() {}

    /** eventClass → ordered list of compiled handlers */
    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, List<GameHandler>> handlerMap = new ConcurrentHashMap<>();

    /**
     * Registers a compiled {@link GameHandler} for the given event type.
     *
     * <p>If this is the first handler for {@code eventType}, a single Bukkit
     * listener is created and registered with the server — all subsequent
     * registrations just append to the same list.
     *
     * @param eventType       the Bukkit event class to listen for
     * @param handler         the compiled lambda to invoke
     * @param plugin          the plugin that owns this registration
     * @param priority        Bukkit event priority
     * @param ignoreCancelled whether to skip already-cancelled events
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Event> void register(
            Class<T> eventType,
            GameHandler<T> handler,
            Plugin plugin,
            EventPriority priority,
            boolean ignoreCancelled
    ) {
        // One atomic insertion: if the key is absent, create the list AND
        // register the Bukkit listener in the same computeIfAbsent block.
        List<GameHandler> handlers = handlerMap.computeIfAbsent(eventType, type -> {
            List<GameHandler> list = new CopyOnWriteArrayList<>();

            // Single dummy Listener per event type
            Listener dummy = new Listener() {};

            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends Event>) type,
                    dummy,
                    priority,
                    (l, event) -> {
                        if (!type.isInstance(event)) return;
                        if (ignoreCancelled && event instanceof Cancellable c && c.isCancelled()) return;
                        for (GameHandler h : list) {
                            h.handle(event);
                        }
                    },
                    plugin,
                    ignoreCancelled
            );

            return list;
        });

        handlers.add(handler);
    }

    /** Convenience overload using NORMAL priority and ignoreCancelled=false. */
    public static <T extends Event> void register(
            Class<T> eventType,
            GameHandler<T> handler,
            Plugin plugin
    ) {
        register(eventType, handler, plugin, EventPriority.NORMAL, false);
    }
}

