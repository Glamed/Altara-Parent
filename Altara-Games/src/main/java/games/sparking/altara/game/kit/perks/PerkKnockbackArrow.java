package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Knockback Arrow</b>
 *
 * <p>Arrows fired by this player have enhanced knockback.
 */
public class PerkKnockbackArrow extends Perk implements Listener {

    private final double power;

    public PerkKnockbackArrow(double power) {
        super("Knockback Arrow", new String[]{"§7Arrows deal §a" + (int)(power * 100) + "% §7extra knockback."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), () -> {
            Vector kb = target.getVelocity().multiply(1.0 + power);
            target.setVelocity(kb);
        }, 1L);
    }
}

