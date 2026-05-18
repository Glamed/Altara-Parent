package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * <b>Wither Arrows</b>
 *
 * <p>Arrows inflict the Wither effect on hit.
 */
public class PerkWitherArrows extends Perk implements Listener {

    private static final int WITHER_DURATION = 80; // 4 seconds
    private static final int WITHER_AMPLIFIER = 0;

    private final Set<Arrow> trackedArrows = Collections.newSetFromMap(new WeakHashMap<>());

    public PerkWitherArrows() {
        super("Wither Arrows", new String[]{
                "§7Arrows inflict §8Wither§7 on enemies."
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
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!trackedArrows.remove(arrow)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER, WITHER_DURATION, WITHER_AMPLIFIER, false, true));
    }
}

