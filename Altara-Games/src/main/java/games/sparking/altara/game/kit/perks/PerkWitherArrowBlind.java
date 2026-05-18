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
 * <b>Wither Arrow Blind</b>
 *
 * <p>Arrows inflict Blindness alongside a brief Wither effect on hit.
 */
public class PerkWitherArrowBlind extends Perk implements Listener {

    private static final int BLIND_DURATION = 60; // 3 seconds
    private static final int WITHER_DURATION = 40; // 2 seconds

    private final Set<Arrow> trackedArrows = Collections.newSetFromMap(new WeakHashMap<>());

    public PerkWitherArrowBlind() {
        super("Wither Arrow Blind", new String[]{
                "§7Arrows inflict §8Wither§7 and §0Blindness§7 on enemies."
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

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, WITHER_DURATION, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, BLIND_DURATION, 0, false, true));
    }
}

