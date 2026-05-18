package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * <b>Iron Shell</b>
 *
 * <p>Reduces all incoming damage by a flat amount.
 */
public class PerkIronShell extends Perk implements Listener {

    private final double reduction;

    public PerkIronShell(double reduction) {
        super("Iron Shell", new String[]{"§7Incoming damage reduced by §a" + reduction + "§7."});
        this.reduction = reduction;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        event.setDamage(Math.max(0, event.getDamage() - reduction));
    }
}

