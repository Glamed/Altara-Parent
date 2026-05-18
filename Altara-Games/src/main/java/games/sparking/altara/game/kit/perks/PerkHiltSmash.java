package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Hilt Smash</b>
 *
 * <p>Occasionally bashes enemies with your weapon hilt, briefly slowing them.
 */
public class PerkHiltSmash extends Perk implements Listener {

    private static final long COOLDOWN_MS = 6000L;
    private static final int SLOW_DURATION = 60; // ticks
    private static final int SLOW_AMPLIFIER = 1;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkHiltSmash() {
        super("Hilt Smash", new String[]{
                "§7Occasionally §9slows§7 enemies on hit.",
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

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOW_DURATION, SLOW_AMPLIFIER, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 0.5f);
    }
}

