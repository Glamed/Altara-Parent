package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Elytra Knockback</b>
 *
 * <p>Hitting an enemy while gliding deals 2× knockback.
 */
public class PerkElytraKnockback extends Perk implements Listener {

    public PerkElytraKnockback() {
        super("Elytra Knockback", new String[]{
                "§7Hitting players while gliding deals §a+100% knockback§7."
        });
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!hasPerk(attacker)) return;
        if (!attacker.isGliding()) return;
        if (!getGame().hasPlayer(attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!getGame().hasPlayer(target)) return;

        // Apply extra knockback after damage is processed
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                games.sparking.altara.AltaraPaper.getPlugin(), () -> {
                    Vector kb = target.getVelocity().multiply(2.0);
                    kb.setY(Math.min(kb.getY(), 1.0));
                    target.setVelocity(kb);
                }, 1L);
    }
}

