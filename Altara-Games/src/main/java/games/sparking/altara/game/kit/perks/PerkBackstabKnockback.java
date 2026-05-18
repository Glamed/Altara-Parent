package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Backstab Knockback</b>
 *
 * <p>When attacking an enemy from behind, deal +2 damage and apply extra knockback.
 */
public class PerkBackstabKnockback extends Perk implements Listener {

    public PerkBackstabKnockback() {
        super("Backstab Knockback", new String[]{
                "§7Attacking from behind deals §a+2 §7damage and extra knockback."
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Vector look = target.getLocation().getDirection().setY(0).normalize();
        Vector from = damager.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0).normalize();
        Vector behind = new Vector(-look.getX(), 0, -look.getZ());

        if (behind.subtract(from).length() < 0.8) {
            event.setDamage(event.getDamage() + 2.0);
            LivingEntity t = target;
            Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), () -> {
                Vector kb = t.getVelocity().add(
                        t.getLocation().toVector().subtract(damager.getLocation().toVector())
                                .setY(0).normalize().multiply(1.5).setY(0.4));
                t.setVelocity(kb);
            }, 1L);
        }
    }
}

