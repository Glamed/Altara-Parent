package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Revealer</b>
 *
 * <p>Periodically reveals invisible players nearby by spawning particles at their location.
 */
public class PerkRevealer extends Perk {

    private static final int INTERVAL_TICKS = 20;
    private static final double RANGE = 16.0;

    private int taskId = -1;

    public PerkRevealer() {
        super("Revealer", new String[]{
                "§7Nearby §cinvisible§7 players are revealed by particles."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPlugin().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPlugin(), () -> {
                    for (Player holder : getGame().getAlivePlayers()) {
                        if (!hasPerk(holder)) continue;
                        for (Player nearby : holder.getWorld().getNearbyEntitiesByType(
                                Player.class, holder.getLocation(), RANGE)) {
                            if (nearby.equals(holder)) continue;
                            if (!getGame().hasPlayer(nearby)) continue;
                            // Check if invisible (has invisibility potion)
                            if (nearby.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                                holder.spawnParticle(Particle.WITCH,
                                        nearby.getLocation().add(0, 1, 0),
                                        5, 0.3, 0.5, 0.3, 0);
                            }
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
}


