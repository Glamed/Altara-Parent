package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * <b>Sharpshooter</b>
 *
 * <p>Consecutive arrow hits deal extra damage, stacking up to 6 times (+2 each).
 * Missing an arrow resets the streak.
 */
public class PerkSharpshooter extends Perk implements Listener {

    private static final int MAX_STACKS = 6;
    private static final double BONUS_PER_STACK = 2.0;

    private final Map<UUID, Integer> hits = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Arrow, UUID> fired = new WeakHashMap<>();

    public PerkSharpshooter() {
        super("Sharpshooter", new String[]{
                "§7Consecutive hits deal §a+2 §7extra damage each.",
                "§7Stacks up to §a6 §7times. Missing resets the bonus."
        });
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        if (event.getProjectile() instanceof Arrow arrow) {
            synchronized (fired) { fired.put(arrow, shooter.getUniqueId()); }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        UUID shooterId;
        synchronized (fired) { shooterId = fired.get(arrow); }
        if (shooterId == null) return;
        Player shooter = org.bukkit.Bukkit.getPlayer(shooterId);
        if (shooter == null || !hasPerk(shooter)) return;
        if (!(event.getEntity() instanceof Player)) return;

        int stacks = Math.min(MAX_STACKS, hits.merge(shooterId, 1, Integer::sum));
        event.setDamage(event.getDamage() + stacks * BONUS_PER_STACK);
    }

    @Override
    public void remove(Player player) {
        hits.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() {
        hits.clear();
        synchronized (fired) { fired.clear(); }
    }
}

