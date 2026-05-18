package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Slow</b>
 *
 * <p>Melee attacks apply a brief Slowness effect to the target.
 */
public class PerkSlow extends Perk implements Listener {

    private final int slowDuration;
    private final int slowAmplifier;

    public PerkSlow(int slowDuration, int slowAmplifier) {
        super("Slow", new String[]{
                "§7Melee hits apply §9Slowness " + (slowAmplifier + 1) + "§7 for §a"
                        + (slowDuration / 20) + "s§7."
        });
        this.slowDuration = slowDuration;
        this.slowAmplifier = slowAmplifier;
    }

    public PerkSlow() {
        this(60, 0); // 3 seconds Slowness I
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!hasPerk(player)) return;

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, slowDuration, slowAmplifier, false, true));
    }
}

