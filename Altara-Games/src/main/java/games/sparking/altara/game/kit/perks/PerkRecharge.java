package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Recharge</b>
 *
 * <p>Dealing melee damage grants a brief burst of haste, rewarding aggressive play.
 */
public class PerkRecharge extends Perk implements Listener {

    private final int hasteDuration;
    private final int hasteAmplifier;

    public PerkRecharge(int hasteDuration, int hasteAmplifier) {
        super("Recharge", new String[]{
                "§7Hitting enemies grants §6Haste " + (hasteAmplifier + 1) + "§7 briefly."
        });
        this.hasteDuration = hasteDuration;
        this.hasteAmplifier = hasteAmplifier;
    }

    public PerkRecharge() {
        this(40, 0); // 2 seconds of Haste I
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!hasPerk(player)) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE, hasteDuration, hasteAmplifier, false, false));
    }
}


