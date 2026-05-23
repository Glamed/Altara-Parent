package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Smasher</b>
 *
 * <p>Melee attacks deal bonus knockback, sending enemies flying.
 */
public class PerkSmasher extends Perk implements Listener {

    private final double multiplier;

    public PerkSmasher(double multiplier) {
        super("Smasher", new String[]{
                "§7Melee attacks deal §c" + (int) (multiplier * 100) + "% bonus knockback§7."
        });
        this.multiplier = multiplier;
    }

    public PerkSmasher() {
        this(1.5);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!hasPerk(player)) return;

        // Apply extra knockback next tick (after base knockback is applied)
        games.sparking.altara.AltaraPaper.getPlugin().getServer().getScheduler()
                .runTaskLater(games.sparking.altara.AltaraPaper.getPlugin(), () -> {
                    Vector base = target.getVelocity();
                    target.setVelocity(new Vector(
                            base.getX() * multiplier,
                            base.getY(),
                            base.getZ() * multiplier));
                }, 1L);

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.7f);
    }
}

