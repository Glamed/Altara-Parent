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

/** <b>Knockback Taken</b> – reduces (or increases) knockback received by this player. */
public class PerkKnockbackTaken extends Perk implements Listener {

    /** Multiplier applied to the player's knockback velocity (e.g. 0.5 = half knockback). */
    private final double knockback;

    public PerkKnockbackTaken(double knockback) {
        super("Knockback", new String[]{"§7You take §a" + (int)(knockback * 100) + "% §7knockback."});
        this.knockback = knockback;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasPerk(victim)) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), () -> {
            Vector v = victim.getVelocity().multiply(knockback);
            victim.setVelocity(v);
        }, 1L);
    }
}

