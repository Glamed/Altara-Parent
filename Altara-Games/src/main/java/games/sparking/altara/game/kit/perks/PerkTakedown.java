package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Takedown</b>
 *
 * <p>Occasionally tackles the target, knocking them to the ground and slowing them.
 */
public class PerkTakedown extends Perk implements Listener {

    private static final long COOLDOWN_MS = 8000L;
    private static final double KNOCKBACK = 0.8;
    private static final int SLOW_DURATION = 60;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkTakedown() {
        super("Takedown", new String[]{
                "§7Occasionally §ctackle§7 your target on hit.",
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

        // Knock them upward and forward
        Vector dir = target.getLocation().subtract(player.getLocation()).toVector()
                .setY(0.5).normalize().multiply(KNOCKBACK);
        target.setVelocity(dir);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOW_DURATION, 1, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
    }
}

