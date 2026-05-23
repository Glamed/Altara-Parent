package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * <b>Radar</b>
 *
 * <p>Periodically sends the player a compass-style direction to nearby enemies via action bar.
 */
public class PerkRadar extends Perk {

    private static final int INTERVAL_TICKS = 40;
    private static final double RANGE = 32.0;

    private int taskId = -1;

    public PerkRadar() {
        super("Radar", new String[]{
                "§7Nearby enemies are shown on your §aaction bar§7."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPlugin().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPlugin(), () -> {
                    for (Player player : getGame().getAlivePlayers()) {
                        if (!hasPerk(player)) continue;
                        List<Player> nearby = player.getWorld().getNearbyEntitiesByType(
                                Player.class, player.getLocation(), RANGE)
                                .stream()
                                .filter(p -> !p.equals(player) && getGame().hasPlayer(p))
                                .toList();

                        String text;
                        if (nearby.isEmpty()) {
                            text = "§aNo enemies in range";
                        } else {
                            StringBuilder sb = new StringBuilder("§cEnemies: ");
                            for (Player enemy : nearby) {
                                double dist = player.getLocation().distance(enemy.getLocation());
                                sb.append("§e").append(enemy.getName())
                                        .append("§f (").append((int) dist).append("m) ");
                            }
                            text = sb.toString();
                        }
                        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
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
}
