package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * <b>Fall Modifier</b>
 *
 * <p>Multiplies fall damage by {@code multiplier}. Use a value &lt;1 to reduce,
 * &gt;1 to increase fall damage.
 */
public class PerkFallModifier extends Perk implements Listener {

    private final double multiplier;

    public PerkFallModifier(double multiplier) {
        super("Feathered Boots", new String[]{"§7You take §a" + (int)(multiplier * 100) + "% §7of normal fall damage."});
        this.multiplier = multiplier;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        event.setDamage(event.getDamage() * multiplier);
    }
}

