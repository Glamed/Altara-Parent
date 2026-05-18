package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * <b>Chain Arrows</b>
 *
 * <p>When an arrow lands, it bounces toward the nearest enemy, up to {@code maxBounces} times.
 */
public class PerkArrowRebound extends Perk implements Listener {

    private final int maxBounces;
    private final double maxDist;

    // arrow → [bounces remaining, already-hit set]
    private final Map<Arrow, int[]> bounceMap = new WeakHashMap<>();
    private final Map<Arrow, Set<UUID>> hitMap = new WeakHashMap<>();

    public PerkArrowRebound(int maxBounces, double maxDist) {
        super("Chain Arrows", new String[]{
                "§7Arrows bounce to nearby enemies up to §a" + maxBounces + " §7times."
        });
        this.maxBounces = maxBounces;
        this.maxDist = maxDist;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        if (event.getProjectile() instanceof Arrow arrow) {
            synchronized (bounceMap) {
                bounceMap.put(arrow, new int[]{maxBounces});
                hitMap.put(arrow, new HashSet<>(Set.of(shooter.getUniqueId())));
            }
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        int[] bounces;
        Set<UUID> alreadyHit;
        synchronized (bounceMap) {
            bounces = bounceMap.remove(arrow);
            alreadyHit = hitMap.remove(arrow);
        }
        if (bounces == null || bounces[0] <= 0) return;

        // Find nearest not-yet-hit player
        Player target = null;
        double best = maxDist;
        for (Entity e : arrow.getNearbyEntities(maxDist, maxDist, maxDist)) {
            if (!(e instanceof Player p)) continue;
            if (alreadyHit.contains(p.getUniqueId())) continue;
            if (!getGame().hasPlayer(p)) continue;
            double d = arrow.getLocation().distance(p.getLocation());
            if (d < best) { best = d; target = p; }
        }
        if (target == null) return;

        alreadyHit.add(target.getUniqueId());
        Player finalTarget = target;
        Arrow bounce = arrow.getWorld().spawnArrow(
                arrow.getLocation(),
                finalTarget.getLocation().add(0, 1, 0).toVector()
                        .subtract(arrow.getLocation().toVector()).normalize(),
                1.5f, 0f);
        bounce.setShooter(arrow.getShooter());
        synchronized (bounceMap) {
            bounceMap.put(bounce, new int[]{bounces[0] - 1});
            hitMap.put(bounce, new HashSet<>(alreadyHit));
        }
    }

    @Override
    public void onUnregister() {
        synchronized (bounceMap) { bounceMap.clear(); hitMap.clear(); }
    }
}

