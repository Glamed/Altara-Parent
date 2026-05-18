package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Fire Arrows</b>
 *
 * <p>Arrows shot by this player set targets on fire for {@code fireTicks} ticks.
 */
public class PerkFireArrows extends Perk implements Listener {

    private final int fireTicks;

    public PerkFireArrows(int fireTicks) {
        super("Fire Arrows", new String[]{"§7Arrows §aset enemies on fire§7."});
        this.fireTicks = fireTicks;
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        event.getEntity().setFireTicks(fireTicks);
    }
}

