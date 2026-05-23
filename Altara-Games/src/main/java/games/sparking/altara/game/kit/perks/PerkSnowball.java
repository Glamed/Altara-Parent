package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Snowball</b>
 *
 * <p>Periodically replenishes snowballs in the player's inventory.
 */
public class PerkSnowball extends Perk {

    private static final int INTERVAL_TICKS = 100; // 5 seconds
    private static final int MAX_SNOWBALLS = 16;

    private int taskId = -1;

    public PerkSnowball() {
        super("Snowball", new String[]{
                "§7Periodically receive §bsnowballs§7 to throw at enemies."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPlugin().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPlugin(), () -> {
                    for (Player player : getGame().getAlivePlayers()) {
                        if (!hasPerk(player)) continue;
                        int count = countSnowballs(player);
                        if (count < MAX_SNOWBALLS) {
                            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
                        }
                    }
                }, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    @Override
    public void onUnregister() {
        if (taskId != -1) {
            AltaraPaper.getPlugin().getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private int countSnowballs(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SNOWBALL) {
                total += item.getAmount();
            }
        }
        return total;
    }
}


