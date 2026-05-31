package games.sparking.altara.hologram.listener;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramService;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.utils.timebased.TimeBasedContainer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles world-load spawning and hologram click detection.
 * All visibility management is handled by per-player entity spawning —
 * no packet tricks required.
 */
@RequiredArgsConstructor
public class HologramListener implements Listener {

    private final HologramService hologramService;

    private final TimeBasedContainer<UUID> clickCooldown =
            new TimeBasedContainer<>(500, TimeUnit.MILLISECONDS);

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        hologramService.loadWorld(event.getWorld());
    }

    /**
     * Spawns each hologram's entities for the joining player.
     * A 2-tick delay is used to let the entity tracker initialise first.
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

    /** Removes every hologram's entities for the leaving player. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Hologram hologram : HologramService.getHolograms()) {
            hologram.despawnFor(player);
        }
    }

    /** Right-click a hologram entity. */
    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand
                || event.getRightClicked() instanceof Interaction)) return;

        Player player = event.getPlayer();
        for (Hologram hologram : HologramService.getHolograms()) {
            if (hologram.isHologramEntity(player, event.getRightClicked())) {
                event.setCancelled(true);
                if (hologram.getClickHandler() == null) return;
                if (clickCooldown.contains(player.getUniqueId())) return;

                int lineIndex = hologram.getClickedLineIndex(player, event.getRightClicked());
                hologram.getClickHandler().click(player, hologram, lineIndex,
                        HologramClickHandler.ClickType.RIGHT_CLICK);
                clickCooldown.add(player.getUniqueId());
                return;
            }
        }
    }

    /** Left-click (attack) a hologram entity. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof ArmorStand
                || event.getEntity() instanceof Interaction)) return;

        for (Hologram hologram : HologramService.getHolograms()) {
            if (hologram.isHologramEntity(player, event.getEntity())) {
                event.setCancelled(true);
                if (hologram.getClickHandler() == null) return;
                if (clickCooldown.contains(player.getUniqueId())) return;

                int lineIndex = hologram.getClickedLineIndex(player, event.getEntity());
                hologram.getClickHandler().click(player, hologram, lineIndex,
                        HologramClickHandler.ClickType.LEFT_CLICK);
                clickCooldown.add(player.getUniqueId());
                return;
            }
        }
    }
}
