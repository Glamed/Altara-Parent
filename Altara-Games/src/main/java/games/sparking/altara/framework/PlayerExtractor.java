package games.sparking.altara.framework;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Caches per-event-class player-extraction functions so the event pipeline
 * never performs an {@code instanceof} chain or reflective lookup at runtime.
 *
 * <p>Common Bukkit events are pre-registered in the static block.
 * Games/modules can add their own extractors via {@link #register}.
 *
 * <p>First lookup for an unknown event walks the cache for a supertype match
 * and stores the result — subsequent calls are a single map get.
 */
public final class PlayerExtractor {

    private PlayerExtractor() {}

    private static final Map<Class<?>, Function<Event, Player>> CACHE = new ConcurrentHashMap<>();

    static {
        // PlayerEvent covers the vast majority of player-specific events
        register(PlayerEvent.class, e -> ((PlayerEvent) e).getPlayer());

        // Entity events — extract the entity if it is a Player (the victim)
        register(EntityDamageEvent.class, e -> {
            EntityDamageEvent ev = (EntityDamageEvent) e;
            return ev.getEntity() instanceof Player p ? p : null;
        });

        register(EntityDamageByEntityEvent.class, e -> {
            EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) e;
            return ev.getEntity() instanceof Player p ? p : null;
        });

        // Inventory events
        register(InventoryClickEvent.class, e -> {
            InventoryClickEvent ev = (InventoryClickEvent) e;
            return ev.getWhoClicked() instanceof Player p ? p : null;
        });

        register(InventoryDragEvent.class, e -> {
            InventoryDragEvent ev = (InventoryDragEvent) e;
            return ev.getWhoClicked() instanceof Player p ? p : null;
        });

        // Any other entity event (deaths, food, combust...) — the entity, if it's a player
        register(EntityEvent.class, e -> ((EntityEvent) e).getEntity() instanceof Player p ? p : null);

        register(BlockBreakEvent.class, e -> ((BlockBreakEvent) e).getPlayer());
        register(BlockPlaceEvent.class, e -> ((BlockPlaceEvent) e).getPlayer());
    }

    /**
     * Registers a custom player extraction function for the given event type.
     * Call this before any events of that type are dispatched (i.e., during startup).
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> void register(Class<T> eventType, Function<T, Player> extractor) {
        CACHE.put(eventType, (Function<Event, Player>) (Function<?, Player>) extractor);
    }

    /**
     * Extracts the relevant player from an event.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Exact hit in the cache → O(1)</li>
     *   <li>Walk the cache for a matching supertype; store the result for next time</li>
     *   <li>Return {@code null} if no extractor is found</li>
     * </ol>
     */
    public static Player extract(Event event) {
        Class<?> type = event.getClass();

        Function<Event, Player> fn = CACHE.get(type);
        if (fn != null) {
            return fn.apply(event);
        }

        // Nearest registered superclass wins; cached so this walk happens once per event class.
        for (Class<?> parent = type.getSuperclass(); parent != null && parent != Event.class; parent = parent.getSuperclass()) {
            Function<Event, Player> inherited = CACHE.get(parent);
            if (inherited != null) {
                CACHE.put(type, inherited);
                return inherited.apply(event);
            }
        }
        CACHE.put(type, e -> null);
        return null;
    }
}

