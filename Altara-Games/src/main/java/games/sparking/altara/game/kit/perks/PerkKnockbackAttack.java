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
 * <b>Melee Knockback</b>
 *
 * <p>Multiplies knockback applied on melee attacks.
 */
public class PerkKnockbackAttack extends Perk implements Listener {

    private final double power;

    public PerkKnockbackAttack(double power) {
        super("Melee Knockback", new String[]{"§7Melee attacks deal §a" + (int)(power * 100) + "% §7knockback."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), () -> {
            Vector kb = target.getVelocity().multiply(1.0 + power);
            target.setVelocity(kb);
        }, 1L);
    }
}

