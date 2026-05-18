package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Champion</b>
 *
 * <p>The player deals +1 damage for each kill they accumulate during the game.
 */
public class PerkChampion extends Perk implements Listener {

    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();

    public PerkChampion() {
        super("Champion", new String[]{"§7You get §astronger §7with each kill."});
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!hasPerk(killer)) return;
        kills.merge(killer.getUniqueId(), 1, Integer::sum);
        int total = kills.get(killer.getUniqueId());
        killer.sendMessage("§6Champion §7– Bonus Damage: §a" + total);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        int bonus = kills.getOrDefault(damager.getUniqueId(), 0);
        if (bonus > 0) event.setDamage(event.getDamage() + bonus);
    }

    @Override
    public void remove(Player player) {
        kills.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() {
        kills.clear();
    }
}

