package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Vanishing Act</b>
 *
 * <p>Become invisible briefly after killing an enemy player. Attacking removes invisibility.
 */
public class PerkVanishing extends Perk implements Listener {

    private static final int DURATION_TICKS = 24; // ~1.2 seconds

    private final Set<UUID> invisible = ConcurrentHashMap.newKeySet();

    public PerkVanishing() {
        super("Vanishing Act", new String[]{
                "§7Become §ainvisible §7for §a1.2s §7after killing an enemy.",
                "§7Attacking removes invisibility."
        });
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!hasPerk(killer)) return;

        invisible.add(killer.getUniqueId());
        killer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATION_TICKS, 0, true, false, false));
        org.bukkit.Bukkit.getScheduler().runTaskLater(games.sparking.altara.AltaraPaper.getPlugin(),
                () -> invisible.remove(killer.getUniqueId()), DURATION_TICKS + 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!invisible.contains(damager.getUniqueId())) return;
        invisible.remove(damager.getUniqueId());
        damager.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void onUnregister() {
        invisible.clear();
    }
}

