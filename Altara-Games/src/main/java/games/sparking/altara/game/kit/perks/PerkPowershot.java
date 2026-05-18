package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
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
 * <b>Power Shot</b>
 *
 * <p>Right-clicking with the bow charges an arrow. The longer you hold, the more damage.
 * The charge counter increments each tick while right-clicking, capped at {@code max} ticks.
 * At max charge, arrows deal up to +15 bonus damage.
 */
public class PerkPowershot extends Perk implements Listener {

    private static final double MAX_BONUS = 15.0;

    private final Map<Arrow, Double> chargedArrows = new WeakHashMap<>();

    public PerkPowershot() {
        super("Power Shot", new String[]{
                "§eCharge §7your bow for §apower shot§7.",
                "§7Up to §a+15 §7bonus damage at full charge."
        });
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        // force is 0.0 (tap) to 1.0 (full draw)
        double bonus = event.getForce() * MAX_BONUS;
        if (event.getProjectile() instanceof Arrow arrow) {
            synchronized (chargedArrows) { chargedArrows.put(arrow, bonus); }
        }
        if (bonus > 5.0) shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 2f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        Double bonus;
        synchronized (chargedArrows) { bonus = chargedArrows.remove(arrow); }
        if (bonus != null && bonus > 0) event.setDamage(event.getDamage() + bonus);
    }

    @Override
    public void onUnregister() {
        synchronized (chargedArrows) { chargedArrows.clear(); }
    }
}

