package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * <b>Sparkler</b>
 *
 * <p>Leaves a trail of firework spark particles behind the player while moving.
 */
public class PerkSparkler extends Perk {

    private static final int INTERVAL_TICKS = 2;

    private int taskId = -1;

    public PerkSparkler() {
        super("Sparkler", new String[]{
                "§7You leave a §etwinkling trail§7 of sparks as you move."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPlugin().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPlugin(), () -> {
                    for (Player player : getGame().getAlivePlayers()) {
                        if (!hasPerk(player)) continue;
                        player.getWorld().spawnParticle(
                                Particle.FIREWORK,
                                player.getLocation().add(0, 0.1, 0),
                                3, 0.2, 0.1, 0.2, 0.02);
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


