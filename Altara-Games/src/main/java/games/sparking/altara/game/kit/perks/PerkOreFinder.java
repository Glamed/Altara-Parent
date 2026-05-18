package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * <b>Ore Finder</b>
 *
 * <p>Periodically spawns particles on nearby ore blocks, revealing them through terrain.
 */
public class PerkOreFinder extends Perk {

    private static final int INTERVAL_TICKS = 40;
    private static final int RANGE = 8;

    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE,
            Material.DIAMOND_ORE, Material.EMERALD_ORE, Material.LAPIS_ORE,
            Material.REDSTONE_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE
    );

    private int taskId = -1;

    public PerkOreFinder() {
        super("Ore Finder", new String[]{
                "§7Nearby §eores§7 are highlighted with particles."
        });
    }

    @Override
    public void onRegister() {
        taskId = AltaraPaper.getPaperInstance().getServer().getScheduler()
                .scheduleSyncRepeatingTask(AltaraPaper.getPaperInstance(), () -> {
                    for (Player player : getGame().getAlivePlayers()) {
                        if (!hasPerk(player)) continue;
                        org.bukkit.Location loc = player.getLocation();
                        for (int dx = -RANGE; dx <= RANGE; dx++) {
                            for (int dy = -RANGE; dy <= RANGE; dy++) {
                                for (int dz = -RANGE; dz <= RANGE; dz++) {
                                    org.bukkit.block.Block block = loc.getWorld().getBlockAt(
                                            loc.getBlockX() + dx,
                                            loc.getBlockY() + dy,
                                            loc.getBlockZ() + dz);
                                    if (ORES.contains(block.getType())) {
                                        player.spawnParticle(Particle.CRIT,
                                                block.getLocation().add(0.5, 0.5, 0.5),
                                                2, 0.2, 0.2, 0.2, 0);
                                    }
                                }
                            }
                        }
                    }
                }, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    @Override
    public void onUnregister() {
        if (taskId != -1) {
            AltaraPaper.getPaperInstance().getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}


