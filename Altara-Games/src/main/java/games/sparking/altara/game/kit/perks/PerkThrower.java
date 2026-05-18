package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Thrower</b>
 *
 * <p>On hit, launches the target into the air above you.
 */
public class PerkThrower extends Perk implements Listener {

    private static final long COOLDOWN_MS = 6000L;
    private static final double THROW_POWER = 1.4;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkThrower() {
        super("Thrower", new String[]{
                "§7Occasionally §c throws§7 your target upward on hit.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!hasPerk(player)) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        target.setVelocity(new Vector(0, THROW_POWER, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.5f);
    }
}

