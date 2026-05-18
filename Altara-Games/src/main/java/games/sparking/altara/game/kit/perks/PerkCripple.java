package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Cripple</b>
 *
 * <p>Melee hits apply Slowness to the target.
 */
public class PerkCripple extends Perk implements Listener {

    private final int power;
    private final int durationSeconds;

    public PerkCripple(int power, int durationSeconds) {
        super("Cripple", new String[]{"§7Attacks apply §aSlow " + power + " §7for §a" + durationSeconds + "s§7."});
        this.power = power;
        this.durationSeconds = durationSeconds;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, power - 1, true, false, false));
    }
}

