package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * <b>No Fall Damage</b>
 *
 * <p>Cancels any {@link EntityDamageEvent.DamageCause#FALL} damage for players
 * who have this perk active.
 */
public class PerkNoFallDamage extends Perk implements Listener {

    public PerkNoFallDamage() {
        super("No Fall Damage", new String[]{"§7You take §ano fall damage§7."});
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}

