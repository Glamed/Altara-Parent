package games.sparking.altara.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import games.sparking.altara.hologram.HologramService;
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
 * Central service that manages the lifecycle of all {@link NPC} instances.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Ensures EntityLib is initialised (delegates to {@link HologramService}).</li>
 *   <li>Registers a PacketEvents {@code INTERACT_ENTITY} listener that routes clicks to
 *       the correct {@link NPCClickHandler} via a fast entity-ID lookup.</li>
 *   <li>Registers a Bukkit player-join/quit listener that auto-spawns and cleans up
 *       NPCs as players enter or leave.</li>
 *   <li>Maintains a registry of all live {@link NPC} objects.</li>
 * </ul>
 *
 * <h2>Initialisation</h2>
 * <pre>{@code
 * // Call once in your plugin's onEnable(), before creating any NPCs.
 * // This also covers HologramService initialisation – you do not need to call both.
 * NPCService.init(this);
 * }</pre>
 *
 * <p>NPCs register themselves automatically when built via {@link NPCBuilder#build()} —
 * you do not need to call {@link #register(NPC)} manually.
 */
public final class NPCService {

    // =========================================================================
    // Registry
    // =========================================================================

    private static final List<NPC> NPCS = new ArrayList<>();

    /**
     * Maps every EntityLib entity ID (body or nametag line) back to its parent {@link NPC}.
     * This is the O(1) lookup used by the PacketEvents click listener.
     */
    private static final Map<Integer, NPC> ENTITY_ID_MAP = new ConcurrentHashMap<>();

    private static boolean initialised = false;

    private NPCService() {}

    // =========================================================================
    // Initialisation
    // =========================================================================

    /**
     * Initialises the NPC subsystem.  Calling this method also satisfies the
     * {@link HologramService} initialisation requirement — you only need to call one.
     *
     * <p>Idempotent: safe to call multiple times.
     *
     * @param plugin your plugin instance
     */
    public static void init(JavaPlugin plugin) {
        if (initialised) return;
        initialised = true;

        // Ensure EntityLib + PacketEvents are initialised (HologramService owns this)
        HologramService.ensureEntityLibInitialized(plugin);

        // -- PacketEvents click listener --------------------------------------
        PacketEvents.getAPI().getEventManager().registerListener(new NPCInteractListener());

        // -- Bukkit join/quit listener ----------------------------------------
        Bukkit.getPluginManager().registerEvents(new NPCBukkitListener(), plugin);
    }

    // =========================================================================
    // NPC registry  (called internally by NPC constructor)
    // =========================================================================

    /** Registers an NPC. Called automatically by the {@link NPC} constructor. */
    static void register(NPC npc) {
        NPCS.add(npc);
    }

    /**
     * Despawns the NPC for all current viewers, removes it from the registry, and
     * releases its EntityLib entity.  After this call the NPC should no longer be used.
     */
    public static void unregister(NPC npc) {
        npc.despawn();
        NPCS.remove(npc);
    }

    /** Unregisters and destroys all NPCs. */
    public static void unregisterAll() {
        new ArrayList<>(NPCS).forEach(NPCService::unregister);
    }

    // =========================================================================
    // Entity-ID mapping  (called internally by NPC)
    // =========================================================================

    static void registerEntityId(int entityId, NPC npc) {
        ENTITY_ID_MAP.put(entityId, npc);
    }

    static void unregisterEntityId(int entityId) {
        ENTITY_ID_MAP.remove(entityId);
    }

    // =========================================================================
    // Read-only view
    // =========================================================================

    /** Returns an unmodifiable snapshot of all currently registered NPCs. */
    public static List<NPC> getNpcs() {
        return List.copyOf(NPCS);
    }

    // =========================================================================
    // Inner – PacketEvents click listener
    // =========================================================================

    private static final class NPCInteractListener extends PacketListenerAbstract {

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            int entityId = packet.getEntityId();

            NPC npc = ENTITY_ID_MAP.get(entityId);
            if (npc == null || npc.getClickHandler() == null) return;

            UUID playerUuid = event.getUser().getUUID();
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) return;

            NPCClickHandler.ClickType clickType =
                    packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK
                            ? NPCClickHandler.ClickType.LEFT_CLICK
                            : NPCClickHandler.ClickType.RIGHT_CLICK;

            npc.getClickHandler().click(player, npc, clickType);
        }
    }

    // =========================================================================
    // Inner – Bukkit join/quit listener
    // =========================================================================

    private static final class NPCBukkitListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            for (NPC npc : NPCS) {
                npc.spawn(player);
            }
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            for (NPC npc : NPCS) {
                npc.despawn(player);
            }
        }
    }
}
