package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Lifesteal Arrows</b>
 *
 * <p>Heals the shooter for {@code health} half-hearts when their arrow hits an enemy.
 */
public class PerkLifestealArrows extends Perk implements Listener {

    private final double health;

    public PerkLifestealArrows(double health) {
        super("Lifesteal Arrows", new String[]{"§7Arrow hits heal you for §a" + health + " §7health."});
        this.health = health;
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        if (!(event.getEntity() instanceof Player)) return;
        shooter.setHealth(Math.min(shooter.getMaxHealth(), shooter.getHealth() + health));
        shooter.getWorld().spawnParticle(Particle.HEART, shooter.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
    }
}

