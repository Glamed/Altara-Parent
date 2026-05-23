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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Knockback</b>
 *
 * <p>Applies enhanced knockback on melee attacks with a short per-target cooldown.
 */
public class PerkKnockback extends Perk implements Listener {

    private final double power;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkKnockback(double power) {
        super("Knockback", new String[]{"§7Attacks knockback enemies with §a" + power + "× §7power."});
        this.power = power;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        UUID key = target.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(key, 0L) < 400) return;
        cooldowns.put(key, now);

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            Vector dir = target.getLocation().toVector()
                    .subtract(damager.getLocation().toVector()).setY(0).normalize().multiply(power).setY(0.3);
            target.setVelocity(dir);
        }, 1L);
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
    }
}

