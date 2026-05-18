package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
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
 * <b>Pinned</b>
 *
 * <p>Heavy hits occasionally root the target in place (slowness V / jump suppression).
 */
public class PerkPinned extends Perk implements Listener {

    private static final long COOLDOWN_MS = 7000L;
    private static final int PIN_TICKS = 40; // 2 seconds

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkPinned() {
        super("Pinned", new String[]{
                "§7Occasionally §cpin§7 your target in place on hit.",
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

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PIN_TICKS, 4, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, PIN_TICKS, 128, false, false));
    }
}

