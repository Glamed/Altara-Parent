package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Wither Attack</b>
 *
 * <p>Melee attacks inflict the Wither effect on the target.
 */
public class PerkWitherAttack extends Perk implements Listener {

    private final int witherDuration;
    private final int witherAmplifier;

    public PerkWitherAttack(int witherDuration, int witherAmplifier) {
        super("Wither Attack", new String[]{
                "§7Melee hits inflict §8Wither " + (witherAmplifier + 1) + "§7 for §a"
                        + (witherDuration / 20) + "s§7."
        });
        this.witherDuration = witherDuration;
        this.witherAmplifier = witherAmplifier;
    }

    public PerkWitherAttack() {
        this(80, 0); // 4 seconds Wither I
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!hasPerk(player)) return;

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER, witherDuration, witherAmplifier, false, true));
    }
}

