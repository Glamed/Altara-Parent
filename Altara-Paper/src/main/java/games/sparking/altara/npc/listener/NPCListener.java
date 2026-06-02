package games.sparking.altara.npc.listener;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.npc.NPC;
import games.sparking.altara.npc.NPCService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Bukkit listener responsible for:
 * <ul>
 *   <li>Spawning NPC packets for players when they join.</li>
 *   <li>Cleaning up NPC state when players leave.</li>
 *   <li>Loading config-backed NPCs when a world loads.</li>
 * </ul>
 *
 * <p>Click detection is handled separately by
 * {@link NPCClickListener} (a PacketEvents listener).
 */
@RequiredArgsConstructor
public class NPCListener implements Listener {

    private final NPCService npcService;

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        npcService.loadWorld(event.getWorld());
    }

    /**
     * Spawns all NPC packets for the joining player after a short delay to
     * let the client settle before entity packets arrive.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            if (!player.isOnline()) return;
            for (NPC npc : NPCService.getNpcs()) {
                npc.spawnFor(player);
            }
        }, 2L);
    }

    /** Cleans up all NPC viewer state for the leaving player. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (NPC npc : NPCService.getNpcs()) {
            npc.despawnFor(event.getPlayer());
        }
    }
}
