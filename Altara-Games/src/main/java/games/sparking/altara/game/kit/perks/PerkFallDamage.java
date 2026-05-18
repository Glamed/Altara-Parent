package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * <b>Fall Damage</b>
 *
 * <p>Adds a flat modifier to fall damage (use negative to reduce, positive to increase).
 */
public class PerkFallDamage extends Perk implements Listener {

    private final double mod;

    public PerkFallDamage(double mod) {
        super("Feather Falling", new String[]{"§7Fall damage is §a" + (mod >= 0 ? "+" : "") + mod + "§7."});
        this.mod = mod;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        double newDamage = Math.max(0, event.getDamage() + mod);
        event.setDamage(newDamage);
    }
}

