package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Rewind</b>
 *
 * <p>Sneak to teleport back to your location from 3 seconds ago.
 */
public class PerkRewind extends Perk implements Listener {

    private static final long COOLDOWN_MS = 12000L;
    private static final int HISTORY_TICKS = 60; // 3 seconds of history

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Location> savedLocations = new ConcurrentHashMap<>();
    private int taskId = -1;

    public PerkRewind() {
        super("Rewind", new String[]{
                "§7Sneak to §bwarp back§7 to where you were 3 seconds ago.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPaperInstance().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPaperInstance(), () -> {
                    for (Player player : getGame().getAlivePlayers()) {
                        if (hasPerk(player)) {
                            savedLocations.put(player.getUniqueId(), player.getLocation().clone());
                        }
                    }
                }, HISTORY_TICKS, HISTORY_TICKS);
    }

    @Override
    public void onUnregister() {
        if (taskId != -1) {
            AltaraPaper.getPaperInstance().getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        savedLocations.clear();
        cooldowns.clear();
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;

        Location saved = savedLocations.get(player.getUniqueId());
        if (saved == null) return;

        cooldowns.put(player.getUniqueId(), now);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 20, 0.5, 1, 0.5, 0.1);
        player.teleport(saved);
        player.getWorld().spawnParticle(Particle.PORTAL, saved, 20, 0.5, 1, 0.5, 0.1);
        player.getWorld().playSound(saved, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }
}


