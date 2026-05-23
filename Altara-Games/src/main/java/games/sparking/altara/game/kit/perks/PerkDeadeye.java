package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Dead Eye</b>
 *
 * <p>Arrows shot by this player will home toward gliding players within
 * {@value RANGE} blocks. The target is notified when locked on.
 */
public class PerkDeadeye extends Perk implements Listener {

    private static final int RANGE = 10;
    private static final int LIFETIME_TICKS = 300; // 15 seconds

    /** Map of active homing arrows → data. */
    private final Map<Arrow, HomingData> arrows = new ConcurrentHashMap<>();

    public PerkDeadeye() {
        super("Dead Eye", new String[]{
                "§7Shot arrows §ahome toward §7nearby gliding enemies."
        });
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        HomingData data = new HomingData(shooter);
        arrows.put(arrow, data);
        startHoming(arrow, data);
    }

    @Override
    public void onUnregister() {
        arrows.keySet().forEach(org.bukkit.entity.Entity::remove);
        arrows.clear();
    }

    private void startHoming(Arrow arrow, HomingData data) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (++ticks > LIFETIME_TICKS || !arrow.isValid() || arrow.isOnGround()) {
                    arrows.remove(arrow);
                    cancel();
                    return;
                }

                // Find target if none
                if (data.target == null) {
                    data.target = findGlidingTarget(arrow, data.shooter);
                    if (data.target != null) {
                        Player t = Bukkit.getPlayer(data.target);
                        if (t != null) t.sendMessage("§cA homing arrow is tracking you!");
                    }
                }

                // Steer toward target
                if (data.target != null) {
                    Player target = Bukkit.getPlayer(data.target);
                    if (target == null || !target.isOnline() || !target.isGliding()) {
                        data.target = null;
                        return;
                    }
                    Vector towards = target.getLocation().add(0, 1, 0).toVector()
                            .subtract(arrow.getLocation().toVector()).normalize();
                    double speed = arrow.getVelocity().length();
                    double factor = Math.min(1.0, ticks / 40.0) * 1.8 + 1.5;
                    arrow.setVelocity(towards.multiply(factor));
                }
            }
        }.runTaskTimer(AltaraPaper.getPlugin(), 1L, 1L);
    }

    private UUID findGlidingTarget(Arrow arrow, UUID shooterId) {
        for (Entity nearby : arrow.getNearbyEntities(RANGE, RANGE, RANGE)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.getUniqueId().equals(shooterId)) continue;
            if (!target.isGliding()) continue;
            if (!getGame().hasPlayer(target)) continue;
            var gp = getGame().getGamePlayer(target).orElse(null);
            if (gp == null || !gp.isAlive()) continue;
            return target.getUniqueId();
        }
        return null;
    }

    private static class HomingData {
        final UUID shooter;
        UUID target;

        HomingData(Player shooter) {
            this.shooter = shooter.getUniqueId();
        }
    }
}

