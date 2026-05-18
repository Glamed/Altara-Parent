package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Food</b>
 *
 * <p>Keeps the player's food level locked at a fixed value every half-second.
 */
public class PerkFood extends Perk {

    private final int amount;
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public PerkFood(int amount) {
        super("Full Belly", new String[]{"§7Food level is locked at §a" + amount + "§7."});
        this.amount = amount;
    }

    @Override
    public void apply(Player player) {
        active.add(player.getUniqueId());
        ensureTask();
    }

    @Override
    public void remove(Player player) {
        active.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() {
        active.clear();
        if (task != null) { task.cancel(); task = null; }
    }

    private void ensureTask() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(AltaraPaper.getPaperInstance(), () -> {
            for (UUID id : active) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) p.setFoodLevel(amount);
            }
        }, 0L, 10L);
    }
}

