package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Flaming Sword</b>
 *
 * <p>Melee attacks set the target on fire.
 */
public class PerkFlamingSword extends Perk implements Listener {

    private final int fireTicks;

    public PerkFlamingSword(int fireTicks) {
        super("Flaming Sword", new String[]{
                "§7Melee attacks §cignite§7 your target for §a" + (fireTicks / 20) + "s§7."
        });
        this.fireTicks = fireTicks;
    }

    public PerkFlamingSword() {
        this(60); // 3 seconds
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        Entity target = event.getEntity();
        target.setFireTicks(Math.max(target.getFireTicks(), fireTicks));
    }
}

