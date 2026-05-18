package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Wool Bomb</b>
 *
 * <p>Right-click with wool to throw a "bomb" that scatters wool blocks in a small radius.
 */
public class PerkWoolBomb extends Perk implements Listener {

    private static final long COOLDOWN_MS = 5000L;
    private static final int RADIUS = 2;
    private static final int BLOCK_DURATION_TICKS = 100; // 5 seconds

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkWoolBomb() {
        super("Wool Bomb", new String[]{
                "§7Right-click with wool to scatter §awool blocks§7 around you.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !isWool(item.getType())) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);
        event.setCancelled(true);

        Material woolType = item.getType();
        List<Block> placed = new ArrayList<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (Math.random() < 0.4) continue;
                    Block block = player.getLocation().getBlock().getRelative(dx, dy, dz);
                    if (block.getType() == Material.AIR) {
                        block.setType(woolType);
                        placed.add(block);
                    }
                }
            }
        }

        // Remove blocks after delay
        games.sparking.altara.AltaraPaper.getPaperInstance().getServer().getScheduler()
                .runTaskLater(games.sparking.altara.AltaraPaper.getPaperInstance(), () -> {
                    for (Block block : placed) {
                        if (isWool(block.getType())) {
                            block.setType(Material.AIR);
                        }
                    }
                }, BLOCK_DURATION_TICKS);
    }

    private boolean isWool(Material mat) {
        return mat.name().endsWith("_WOOL");
    }
}

