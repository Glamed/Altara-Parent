package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Wool Cloud</b>
 *
 * <p>Periodically envelopes the player in a cloud of wool blocks that absorb projectiles,
 * then dissolves. (Simplified: creates a ring of wool particles and brief physical blocks.)
 */
public class PerkWoolCloud extends Perk {

    private static final long COOLDOWN_MS = 15000L;
    private static final int INTERVAL_TICKS = (int) (COOLDOWN_MS / 50);
    private static final int CLOUD_RADIUS = 2;
    private static final int CLOUD_DURATION_TICKS = 60;

    private final Map<UUID, Long> nextActivation = new ConcurrentHashMap<>();
    private int taskId = -1;

    public PerkWoolCloud() {
        super("Wool Cloud", new String[]{
                "§7Periodically surrounds you with a §fwool cloud§7 that absorbs damage.",
                "§7Duration: §a3s§7. Interval: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPaperInstance().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPaperInstance(), () -> {
                    for (Player player : getGame().getAlivePlayers()) {
                        if (!hasPerk(player)) continue;
                        long now = System.currentTimeMillis();
                        long next = nextActivation.getOrDefault(player.getUniqueId(), 0L);
                        if (now < next) continue;
                        nextActivation.put(player.getUniqueId(), now + COOLDOWN_MS);

                        spawnCloud(player);
                    }
                }, 20, 20);
    }

    @Override
    public void onUnregister() {
        if (taskId != -1) {
            AltaraPaper.getPaperInstance().getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        nextActivation.clear();
    }

    private void spawnCloud(Player player) {
        List<Block> placed = new ArrayList<>();
        org.bukkit.Location center = player.getLocation();

        for (int dx = -CLOUD_RADIUS; dx <= CLOUD_RADIUS; dx++) {
            for (int dy = 0; dy <= CLOUD_RADIUS; dy++) {
                for (int dz = -CLOUD_RADIUS; dz <= CLOUD_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > CLOUD_RADIUS * CLOUD_RADIUS + 1) continue;
                    Block block = center.getBlock().getRelative(dx, dy, dz);
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.WHITE_WOOL);
                        placed.add(block);
                    }
                }
            }
        }

        // Particle effect
        player.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, 1, 0),
                30, CLOUD_RADIUS, CLOUD_RADIUS, CLOUD_RADIUS, 0.05);

        // Remove cloud after duration
        AltaraPaper.getPaperInstance().getServer().getScheduler()
                .runTaskLater(AltaraPaper.getPaperInstance(), () -> {
                    for (Block block : placed) {
                        if (block.getType() == Material.WHITE_WOOL) {
                            block.setType(Material.AIR);
                        }
                    }
                }, CLOUD_DURATION_TICKS);
    }
}


