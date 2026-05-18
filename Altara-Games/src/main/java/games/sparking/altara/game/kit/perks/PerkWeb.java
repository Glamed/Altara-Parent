package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Web</b>
 *
 * <p>Occasionally places a cobweb at the target's feet on hit, trapping them briefly.
 */
public class PerkWeb extends Perk implements Listener {

    private static final long COOLDOWN_MS = 6000L;
    private static final int WEB_TICKS = 40; // cobweb stays for 2 seconds

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkWeb() {
        super("Web", new String[]{
                "§7Occasionally traps enemies in §fa cobweb§7 on hit.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!hasPerk(player)) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        org.bukkit.block.Block block = target.getLocation().getBlock();
        if (block.getType() == Material.AIR) {
            block.setType(Material.COBWEB);
            // Remove web after delay
            games.sparking.altara.AltaraPaper.getPaperInstance().getServer().getScheduler()
                    .runTaskLater(games.sparking.altara.AltaraPaper.getPaperInstance(), () -> {
                        if (block.getType() == Material.COBWEB) {
                            block.setType(Material.AIR);
                        }
                    }, WEB_TICKS);
        }
    }
}

