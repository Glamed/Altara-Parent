package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * <b>Sniper</b>
 *
 * <p>Arrow damage scales with distance travelled. The further the arrow flies, the more
 * damage it deals.
 */
public class PerkSniper extends Perk implements Listener {

    private static final String META_KEY = "altara_sniper_origin";

    public PerkSniper() {
        super("Sniper", new String[]{
                "§7The further your arrow travels,",
                "§7the §amore damage §7it deals."
        });
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        event.getProjectile().setMetadata(META_KEY,
                new FixedMetadataValue(games.sparking.altara.AltaraPaper.getPaperInstance(),
                        event.getProjectile().getLocation().clone()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata(META_KEY)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;

        Location origin = (Location) arrow.getMetadata(META_KEY).get(0).value();
        double dist = origin.distance(arrow.getLocation()) / 13.0;
        double bonus = Math.max(0, Math.pow(dist, dist) - event.getDamage());
        event.setDamage(event.getDamage() + bonus);
    }
}

