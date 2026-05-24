package games.sparking.altara.npc;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraLobby;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.selector.ServerSelectorEntry;
import games.sparking.altara.selector.ServerSelectorMenu;
import games.sparking.altara.server.ServerInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lobby NPCs built with the Altara NPC system.
 *
 * <p>Each {@link ServerSelectorEntry} that has an {@code npcLocation} configured gets
 * a player-skin NPC spawned at that location. The NPC displays the entry's
 * {@code npcLines} as a per-player nametag hologram (with live placeholder
 * replacement). Clicking the NPC opens the {@link ServerSelectorMenu}.
 */
public class LobbyNPC {

    private final AltaraLobby plugin = AltaraLobby.getLobbyInstance();

    /** Keyed by lower-case server name. */
    private final Map<String, NPC> npcMap = new ConcurrentHashMap<>();

    // =========================================================================
    // Load / spawn
    // =========================================================================

    /** Spawns NPCs for every selector entry that has an {@code npcLocation} set. */
    public void loadNpcs() {
        for (ServerSelectorEntry entry : plugin.getLobbyConfig().getServerSelector()) {
            if (entry.getNpcLocation() == null) continue;
            spawnForEntry(entry);
        }
    }

    /**
     * Spawns (or replaces) the NPC for the given selector entry.
     *
     * <p>Skin fetching is asynchronous — the NPC is built and spawned once the
     * Mojang API responds. If fetching fails the NPC still spawns, just without a skin.
     */
    public void spawnForEntry(ServerSelectorEntry entry) {
        JavaPlugin javaPlugin = AltaraLobby.getPlugin();

        NPCSkin.fetchAsync(entry.getNpcSkin(), javaPlugin, skin -> {
            // Remove any existing NPC for this server before spawning a new one
            despawnEverythingOf(entry.getServerName());

            NPC npc = new NPCBuilder()
                    .at(entry.getNpcLocation().getLocation())
                    .name(stripColor(entry.getName()))
                    .skin(skin) // null-safe: NPCBuilder handles null skin
                    .nametagProvider(player -> buildNametagLines(entry, player))
                    .clickHandler((player, npcObj, clickType) -> {
                        if (clickType == NPCClickHandler.ClickType.RIGHT_CLICK) {
                            new ServerSelectorMenu().openMenu(player);
                        }
                    })
                    .buildAndSpawn();

            npcMap.put(entry.getServerName().toLowerCase(), npc);
        });
    }

    // =========================================================================
    // Despawn
    // =========================================================================

    /**
     * Despawns and unregisters the NPC for the given server.
     * Safe to call even if no NPC exists for that server.
     */
    public void despawnEverythingOf(String serverName) {
        NPC npc = npcMap.remove(serverName.toLowerCase());
        if (npc != null) {
            NPCService.unregister(npc);
        }
    }

    /** Despawns and unregisters all managed NPCs. */
    public void despawnEverything() {
        new ArrayList<>(npcMap.values()).forEach(NPCService::unregister);
        npcMap.clear();
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    /** Returns the NPC for the given server name, or {@code null} if not found. */
    public NPC getNpc(String serverName) {
        return npcMap.get(serverName.toLowerCase());
    }

    // =========================================================================
    // Nametag placeholder helpers
    // =========================================================================

    /**
     * Builds the per-player nametag lines for an entry, resolving all placeholders
     * against the viewer's live server data.
     */
    private List<String> buildNametagLines(ServerSelectorEntry entry, Player player) {
        ServerInfo server = entry.getServer();

        int online     = (server != null) ? server.getOnlinePlayers() : 0;
        int max        = (server != null) ? server.getMaxPlayers()    : 0;
        String state   = (server != null) ? ServerSelectorEntry.getStateName(player, server) : "&cOffline";
        String inQueue = (server != null) ? getQueueStatus(player, server) : "0 in queue";
        String rankScope = (server != null)
                ? Altara.getSharedInstance()
                        .getProfileService()
                        .getProfile(player)
                        .getCurrentGrantOn(server.getName())
                        .asRank()
                        .getDisplayName()
                : "N/A";

        List<String> resolved = new ArrayList<>(entry.getNpcLines().size());
        for (String line : entry.getNpcLines()) {
            resolved.add(line
                    .replace("%online%",        String.valueOf(online))
                    .replace("%max%",           String.valueOf(max))
                    .replace("%status%",        state)
                    .replace("%in_queue%",      inQueue)
                    .replace("%rank_on_scope%", rankScope));
        }
        return resolved;
    }

    private static String getQueueStatus(Player player, ServerInfo server) {
        QueueService queueService = AltaraLobby.getLobbyInstance().getQueueService();
        if (queueService.isQueueingFor(player.getUniqueId(), server.getName())) {
            int pos   = queueService.getPosition(player.getUniqueId(), server.getName());
            int total = queueService.getQueueing(server.getName()).size();
            return String.format("Position %d of %d", pos + 1, total);
        }
        return queueService.getQueueing(server.getName()).size() + " in queue";
    }

    /** Strips Minecraft § and & colour codes so the GameProfile name stays plain. */
    private static String stripColor(String text) {
        return ChatColor.stripColor(text.replace('&', '§'));
    }
}