package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Aeronaught</b>
 *
 * <p>Deals §a+45% bonus damage§r to enemies while the wielder is gliding.
 */
public class PerkAeronaught extends Perk implements Listener {

    private static final double BONUS_MULTIPLIER = 0.45;

    public PerkAeronaught() {
        super("Elytra Damage", new String[]{
                "§7Deal §a+45% §7damage while gliding."
        });
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!hasPerk(attacker)) return;
        if (!attacker.isGliding()) return;
        if (!getGame().hasPlayer(attacker)) return;
        // Only boost against game players
        if (event.getEntity() instanceof Player target && !getGame().hasPlayer(target)) return;

        double bonus = event.getDamage() * BONUS_MULTIPLIER;
        event.setDamage(event.getDamage() + bonus);
    }
}

