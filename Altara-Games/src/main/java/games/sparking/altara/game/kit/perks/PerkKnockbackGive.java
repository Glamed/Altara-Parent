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

/** <b>Knockback Give</b> – attacker's hits give extra knockback to victims. */
public class PerkKnockbackGive extends Perk implements Listener {

    private final double power;

    public PerkKnockbackGive(double power) {
        super("Knockback", new String[]{"§7You deal §a" + (int)(power * 100) + "% §7knockback."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            Vector kb = target.getVelocity().multiply(1.0 + power);
            target.setVelocity(kb);
        }, 1L);
    }
}

