package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * <b>Mammoth</b>
 *
 * <p>Greatly reduces knockback taken. You're a tank — hard to move.
 */
public class PerkMammoth extends Perk implements Listener {

    private final double reductionFactor;

    public PerkMammoth(double reductionFactor) {
        super("Mammoth", new String[]{
                "§7Knockback from attacks is reduced by §a" + (int) (reductionFactor * 100) + "%§7."
        });
        this.reductionFactor = reductionFactor;
    }

    public PerkMammoth() {
        this(0.75); // 75% reduction
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPerk(player)) return;

        // Cancel velocity change next tick
        player.getServer().getScheduler().runTaskLater(
                games.sparking.altara.AltaraPaper.getPlugin(), () -> {
                    Vector vel = player.getVelocity();
                    player.setVelocity(new Vector(
                            vel.getX() * (1 - reductionFactor),
                            vel.getY(),
                            vel.getZ() * (1 - reductionFactor)
                    ));
                }, 1L);
    }
}

