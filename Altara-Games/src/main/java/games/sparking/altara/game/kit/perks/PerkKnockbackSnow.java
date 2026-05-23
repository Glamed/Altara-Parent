package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/** <b>Frosty Knockback</b> – extra knockback when the target is standing on snow. */
public class PerkKnockbackSnow extends Perk implements Listener {

    private final double power;

    public PerkKnockbackSnow(double power) {
        super("Frosty Knockback", new String[]{"§7Deal §a" + (int)(power * 100) + "% §7extra knockback to enemies on snow."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        Material below = target.getLocation().subtract(0, 1, 0).getBlock().getType();
        if (below != Material.SNOW && below != Material.SNOW_BLOCK && below != Material.POWDER_SNOW) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            Vector kb = target.getVelocity().multiply(1.0 + power);
            target.setVelocity(kb);
        }, 1L);
    }
}

