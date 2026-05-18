package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/** <b>Knockback Multiplier</b> – the player receives multiplied knockback when hit. */
public class PerkKnockbackMultiplier extends Perk implements Listener {

    private final double power;

    public PerkKnockbackMultiplier(double power) {
        super("Knockback", new String[]{"§7You take §a" + (int)(power * 100) + "% §7knockback."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasPerk(victim)) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), () -> {
            Vector kb = victim.getVelocity().multiply(power);
            victim.setVelocity(kb);
        }, 1L);
    }
}

