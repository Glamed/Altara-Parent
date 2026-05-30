package games.sparking.altara.hologram.listener;

import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramService;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.utils.timebased.TimeBasedContainer;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles hologram click detection and world-load spawning.
 * Register with Bukkit only — no PacketEvents required.
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

    /** Right-click a hologram armor stand or its interaction entity. */
    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand || event.getRightClicked() instanceof Interaction)) return;

        Player player = event.getPlayer();
        for (Hologram hologram : HologramService.getHolograms()) {
            if (!hologram.isHologramEntity(event.getRightClicked())) continue;

            event.setCancelled(true);
            if (hologram.getClickHandler() == null) return;
            if (clickCooldown.contains(player.getUniqueId())) return;

            hologram.getClickHandler().click(player, hologram, HologramClickHandler.ClickType.RIGHT_CLICK);
            clickCooldown.add(player.getUniqueId());
            return;
        }
    }

    /** Left-click (attack) a hologram armor stand or its interaction entity. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof ArmorStand || event.getEntity() instanceof Interaction)) return;

        for (Hologram hologram : HologramService.getHolograms()) {
            if (!hologram.isHologramEntity(event.getEntity())) continue;

            event.setCancelled(true);
            if (hologram.getClickHandler() == null) return;
            if (clickCooldown.contains(player.getUniqueId())) return;

            hologram.getClickHandler().click(player, hologram, HologramClickHandler.ClickType.LEFT_CLICK);
            clickCooldown.add(player.getUniqueId());
            return;
        }
    }
}
