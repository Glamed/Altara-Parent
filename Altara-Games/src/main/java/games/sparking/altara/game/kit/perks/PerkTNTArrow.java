package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.WeakHashMap;

/**
 * <b>TNT Arrow</b>
 *
 * <p>Arrows explode on impact, dealing area damage.
 */
public class PerkTNTArrow extends Perk implements Listener {

    private static final float EXPLOSION_POWER = 1.5f;
    private final Set<Arrow> trackedArrows = Collections.newSetFromMap(new WeakHashMap<>());

    public PerkTNTArrow() {
        super("TNT Arrow", new String[]{
                "§7Your arrows §cexplode§7 on impact."
        });
    }

    @EventHandler
    public void onShoot(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        trackedArrows.add(arrow);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!trackedArrows.remove(arrow)) return;

        arrow.getWorld().createExplosion(arrow.getLocation(), EXPLOSION_POWER, false, false);
        arrow.remove();
    }
}

