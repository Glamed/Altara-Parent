package games.sparking.altara.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service that manages the lifecycle of all {@link Hologram} instances.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Initialises EntityLib (once per server startup).</li>
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
     * Initialises EntityLib, registers the PacketEvents click listener, and sets up the
     * Bukkit join/quit listener.
     *
     * <p>This method is idempotent: calling it more than once is a no-op after the first call.
     *
     * @param plugin your plugin instance
     */
    public static void init(JavaPlugin plugin) {
        if (initialised) return;
        initialised = true;

        // -- EntityLib setup (idempotent – safe to call from NPCService too) --
        ensureEntityLibInitialized(plugin);

        // -- PacketEvents click listener --------------------------------------
        PacketEvents.getAPI().getEventManager().registerListener(new HologramInteractListener());

        // -- Bukkit join/quit listener ----------------------------------------
        Bukkit.getPluginManager().registerEvents(new HologramBukkitListener(), plugin);
    }

    /**
     * Initialises EntityLib exactly once.  Both {@link HologramService} and
     * {@link games.sparking.altara.npc.NPCService} call this so that either service
     * can be initialised first without causing a double-init error.
     */
    public static void ensureEntityLibInitialized(JavaPlugin plugin) {
        try {
            EntityLib.init(new SpigotEntityLibPlatform(plugin), PacketEvents.getAPI());
        } catch (IllegalStateException ignored) {
            // Already initialised — safe to continue
        }
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

    // =========================================================================
    // Inner – PacketEvents click listener
    // =========================================================================

    private static final class HologramInteractListener extends PacketListenerAbstract {

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            int entityId = packet.getEntityId();

            Hologram hologram = ENTITY_ID_MAP.get(entityId);
            if (hologram == null || hologram.getClickHandler() == null) return;

            // Resolve the Bukkit Player from the PacketEvents user UUID
            UUID playerUuid = event.getUser().getUUID();
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) return;

            HologramClickHandler.ClickType clickType =
                    packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK
                            ? HologramClickHandler.ClickType.LEFT_CLICK
                            : HologramClickHandler.ClickType.RIGHT_CLICK;

            hologram.getClickHandler().click(player, hologram, clickType);
        }
    }

    // =========================================================================
    // Inner – Bukkit join/quit listener
    // =========================================================================

    private static final class HologramBukkitListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            // Spawn all registered holograms for the joining player (each will check canSee)
            Player player = event.getPlayer();
            for (Hologram hologram : HOLOGRAMS) {
                hologram.spawn(player);
            }
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            // Release all entity resources for the leaving player
            Player player = event.getPlayer();
            for (Hologram hologram : HOLOGRAMS) {
                hologram.despawn(player);
            }
        }
    }
}
