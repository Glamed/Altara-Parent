package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Fletcher</b>
 *
 * <p>Every {@value GIVE_INTERVAL_SECONDS}s, gives the player arrows up to {@value MAX_ARROWS}.
 */
public class PerkFletcher extends Perk implements Listener {

    private static final int GIVE_INTERVAL_SECONDS = 20;
    private static final int MAX_ARROWS = 3;

    /** Players currently tracked by this perk. */
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask task;

    public PerkFletcher() {
        super("Fletcher", new String[]{
                "§7Receive §a" + MAX_ARROWS + " arrows §7every §a" + GIVE_INTERVAL_SECONDS + "s§7."
        });
    }

    @Override
    public void apply(Player player) {
        activePlayers.add(player.getUniqueId());
        ensureTask();
    }

    @Override
    public void remove(Player player) {
        activePlayers.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() {
        activePlayers.clear();
        if (task != null) { task.cancel(); task = null; }
    }

    private void ensureTask() {
        if (task != null) return;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!getKit().getGame().isLive()) return;
                for (UUID id : activePlayers) {
                    Player p = org.bukkit.Bukkit.getPlayer(id);
                    if (p == null || !p.isOnline()) continue;
                    int arrows = countArrows(p);
                    if (arrows < MAX_ARROWS) {
                        p.getInventory().addItem(new ItemStack(Material.ARROW, MAX_ARROWS - arrows));
                    }
                }
            }
        }.runTaskTimer(games.sparking.altara.AltaraPaper.getPaperInstance(),
                GIVE_INTERVAL_SECONDS * 20L, GIVE_INTERVAL_SECONDS * 20L);
    }

    private int countArrows(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                count += item.getAmount();
            }
        }
        return count;
    }
}

