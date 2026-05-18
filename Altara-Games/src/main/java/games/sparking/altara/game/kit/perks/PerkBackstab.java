package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Backstab</b>
 *
 * <p>Deal +2 bonus damage when attacking a player from behind.
 */
public class PerkBackstab extends Perk implements Listener {

    public PerkBackstab() {
        super("Backstab", new String[]{"§7Deal §a+2 §7damage when attacking from behind."});
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Check if attacking from behind
        Vector look = target.getLocation().getDirection().setY(0).normalize();
        Vector from = damager.getLocation().toVector().subtract(target.getLocation().toVector()).setY(0).normalize();
        Vector behind = new Vector(-look.getX(), 0, -look.getZ());

        if (behind.subtract(from).length() < 0.8) {
            event.setDamage(event.getDamage() + 2.0);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 2f);
        }
    }
}

