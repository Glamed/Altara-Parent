package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Flaming Knockback</b>
 *
 * <p>Extra knockback when hitting burning enemies.
 */
public class PerkKnockbackFire extends Perk implements Listener {

    private final double power;

    public PerkKnockbackFire(double power) {
        super("Flaming Knockback", new String[]{"§7Deal §a" + (int)(power * 100) + "% §7extra knockback to burning enemies."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (target.getFireTicks() <= 0) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            Vector kb = target.getVelocity().multiply(1.0 + power);
            target.setVelocity(kb);
        }, 1L);
    }
}

