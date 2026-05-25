package games.sparking.altara.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service that manages the lifecycle of all {@link Hologram} instances.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Registers a PacketEvents listener that maps incoming
 *       {@code INTERACT_ENTITY} packets to hologram click callbacks.</li>
 *   <li>Registers a Bukkit listener that auto-spawns / cleans up holograms as
 *       players join or leave.</li>
 *   <li>Maintains a registry of all live {@link Hologram} objects and a fast
 *       entity-ID → hologram lookup table for click routing.</li>
 * </ul>
 *
 * <h2>Initialisation</h2>
 * <pre>{@code
 * // Call once in your plugin's onEnable(), before creating any holograms:
 * HologramService.init(this);
 * }</pre>
 *
 * <p>Hologram objects register themselves automatically when constructed via
 * {@link HologramBuilder#build()} — you do not need to call {@link #register} manually.
 */
public final class HologramService {

    // =========================================================================
    // Registry
    // =========================================================================

    /** All live holograms. */
    private static final List<Hologram> HOLOGRAMS = new ArrayList<>();

    /**
     * Maps every EntityLib entity ID that belongs to a hologram line back to its
     * parent {@link Hologram}.  This is the fast lookup table used by the click listener.
     */
    private static final Map<Integer, Hologram> ENTITY_ID_MAP = new ConcurrentHashMap<>();

    private static boolean initialised = false;

    // Prevent instantiation
    private HologramService() {}

    // =========================================================================
    // Initialisation
    // =========================================================================

    /**
     * Registers the PacketEvents click listener and the Bukkit join/quit listener.
     *
     * <p>This method is idempotent: calling it more than once is a no-op after the first call.
     *
     * @param plugin your plugin instance
     */
    public static void init(JavaPlugin plugin) {
        if (initialised) return;
        initialised = true;

        // -- PacketEvents click listener --------------------------------------
        PacketEvents.getAPI().getEventManager().registerListener(new HologramInteractListener());

        // -- Bukkit join/quit listener ----------------------------------------
        Bukkit.getPluginManager().registerEvents(new HologramBukkitListener(), plugin);
    }

    // =========================================================================
    // Hologram registry  (called internally by Hologram constructor)
    // =========================================================================

    /** Adds a hologram to the registry. Called automatically by the {@link Hologram} constructor. */
    static void register(Hologram hologram) {
        HOLOGRAMS.add(hologram);
    }

    /**
     * Stops the update task, despawns the hologram for all players, and removes it from
     * the registry.  After this call the hologram should no longer be used.
     */
    public static void unregister(Hologram hologram) {
        hologram.cancelUpdateTask();
        hologram.despawn();
        HOLOGRAMS.remove(hologram);
    }

    /** Unregisters and cleans up all holograms. */
    public static void unregisterAll() {
        new ArrayList<>(HOLOGRAMS).forEach(HologramService::unregister);
    }

    // =========================================================================
    // Entity-ID → Hologram mapping  (called internally by Hologram)
    // =========================================================================

    static void registerEntityId(int entityId, Hologram hologram) {
        ENTITY_ID_MAP.put(entityId, hologram);
    }

    static void unregisterEntityId(int entityId) {
        ENTITY_ID_MAP.remove(entityId);
    }

    /** Returns the hologram that owns the armor-stand with the given entity ID, or {@code null}. */
    static Hologram getHologramForEntityId(int entityId) {
        return ENTITY_ID_MAP.get(entityId);
    }

    // =========================================================================
    // Read-only view of the registry
    // =========================================================================

    /** Returns an unmodifiable snapshot of all currently registered holograms. */
    public static List<Hologram> getHolograms() {
        return List.copyOf(HOLOGRAMS);
    }
}
