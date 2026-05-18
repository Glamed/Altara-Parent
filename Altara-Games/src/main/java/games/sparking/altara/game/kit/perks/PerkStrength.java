package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Strength</b>
 *
 * <p>Adds flat bonus damage to all melee attacks.
 */
public class PerkStrength extends Perk implements Listener {

    private final double power;

    public PerkStrength(double power) {
        super("Strength", new String[]{"§7You deal §a+" + power + " §7bonus damage."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        event.setDamage(event.getDamage() + power);
    }
}

