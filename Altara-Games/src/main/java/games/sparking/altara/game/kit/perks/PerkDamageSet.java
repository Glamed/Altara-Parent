package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Damage Set</b>
 *
 * <p>Sets the damage dealt by a player to a fixed value on every melee hit.
 */
public class PerkDamageSet extends Perk implements Listener {

    private final double damage;

    public PerkDamageSet(double damage) {
        super("Damage Set", new String[]{"§7Attacks always deal §a" + damage + " §7damage."});
        this.damage = damage;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        event.setDamage(damage);
    }
}

