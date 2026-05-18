package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Cleave</b>
 *
 * <p>Melee attacks also deal {@code splash}% damage to players within 4 blocks of the target.
 */
public class PerkCleave extends Perk implements Listener {

    private final double splash;

    public PerkCleave(double splash) {
        super("Cleave", new String[]{"§7Attacks deal §a" + (int)(splash * 100) + "% §7damage to nearby enemies."});
        this.splash = splash;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        double splashDamage = event.getDamage() * splash;
        for (Player nearby : target.getWorld().getNearbyEntitiesByType(Player.class, target.getLocation(), 4.0)) {
            if (nearby.equals(target) || nearby.equals(damager)) continue;
            if (!getGame().hasPlayer(nearby)) continue;
            var gp = getGame().getGamePlayer(nearby).orElse(null);
            if (gp == null || !gp.isAlive()) continue;
            nearby.damage(splashDamage, damager);
        }
    }
}

