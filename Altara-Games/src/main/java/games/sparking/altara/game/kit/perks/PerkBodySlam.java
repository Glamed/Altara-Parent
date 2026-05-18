package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

/**
 * <b>Body Slam</b>
 *
 * <p>When this player lands from a significant fall, all players within 3 blocks of the
 * landing spot take damage proportional to fall distance and get knocked back.
 */
public class PerkBodySlam extends Perk implements Listener {

    private static final double MIN_FALL = 3.0;

    public PerkBodySlam() {
        super("Body Slam", new String[]{"§7Land on enemies to §adamage and knock them back§7."});
    }

    @EventHandler
    public void onLand(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player lander)) return;
        if (!hasPerk(lander)) return;
        if (lander.getFallDistance() < MIN_FALL) return;

        double damage = lander.getFallDistance() * 0.5;

        for (Player nearby : lander.getWorld().getNearbyEntitiesByType(Player.class, lander.getLocation(), 3.0)) {
            if (nearby.equals(lander)) continue;
            if (!getGame().hasPlayer(nearby)) continue;
            var gp = getGame().getGamePlayer(nearby).orElse(null);
            if (gp == null || !gp.isAlive()) continue;
            nearby.damage(damage, lander);
            Vector kb = nearby.getLocation().toVector().subtract(lander.getLocation().toVector()).setY(0).normalize().multiply(1.2).setY(0.6);
            nearby.setVelocity(kb);
        }

        lander.getWorld().playSound(lander.getLocation(), Sound.ENTITY_IRON_GOLEM_STEP, 1f, 0.8f);
    }
}

