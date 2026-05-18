package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Shocking Strike</b>
 *
 * <p>Occasionally calls lightning on an enemy when you hit them (cosmetic only — no extra fire damage).
 */
public class PerkShockingStrike extends Perk implements Listener {

    private static final long COOLDOWN_MS = 5000L;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkShockingStrike() {
        super("Shocking Strike", new String[]{
                "§7Occasionally strikes enemies with §elightning§7.",
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

        // Strike lightning (cosmetic — strikeLightningEffect doesn't cause fire)
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
    }
}


