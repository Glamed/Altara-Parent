package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Ranged Bleeding</b>
 *
 * <p>Being hit by an arrow causes bleeding: the victim takes 1 damage per second for
 * 3 seconds.
 */
public class PerkRangedBleeding extends Perk implements Listener {

    private final Map<UUID, Integer> bleeding = new ConcurrentHashMap<>();

    public PerkRangedBleeding() {
        super("Bleeding", new String[]{
                "§7Arrow hits cause §ableed §7for 3 seconds."
        });
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!hasPerk(shooter)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        UUID id = victim.getUniqueId();
        bleeding.put(id, 3);

        applyBleeding(victim, id, 3);
    }

    private void applyBleeding(Player victim, UUID id, int ticks) {
        if (ticks <= 0) { bleeding.remove(id); return; }
        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            Integer remaining = bleeding.get(id);
            if (remaining == null || remaining <= 0) return;
            bleeding.put(id, remaining - 1);
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.damage(1.0);
                applyBleeding(p, id, ticks - 1);
            } else {
                bleeding.remove(id);
            }
        }, 20L);
    }

    @Override
    public void onUnregister() {
        bleeding.clear();
    }
}

