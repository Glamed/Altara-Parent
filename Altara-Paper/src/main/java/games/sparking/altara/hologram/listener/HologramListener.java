package games.sparking.altara.hologram.listener;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramService;
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
 *   <li>Spawning hologram packets for players when they join.</li>
 *   <li>Destroying hologram packets when players leave.</li>
 *   <li>Loading config-backed holograms when a world loads.</li>
 * </ul>
 *
 * <p>Click detection is handled separately by
 * {@link HologramClickListener} (a PacketEvents listener).
 */
@RequiredArgsConstructor
public class HologramListener implements Listener {

    private final HologramService hologramService = AltaraPaper.getPaperInstance().getHologramService();

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        hologramService.loadWorld(event.getWorld());
    }

    /**
     * Sends hologram packets to the joining player after a short delay to
     * allow the client's entity tracker to initialise.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            if (!player.isOnline()) return;
            for (Hologram hologram : HologramService.getHolograms()) {
                if (hologram.getViewers().isEmpty()
                        || hologram.getViewers().contains(player.getUniqueId())) {
                    hologram.spawnFor(player);
                }
            }
        }, 2L);
    }

    /** Destroys all hologram packet-entities for the leaving player. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (Hologram hologram : HologramService.getHolograms()) {
            hologram.despawnFor(event.getPlayer());
        }
    }
}
