package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Ice Bridge</b>
 *
 * <p>Right-click the Ice block item to launch the player upward and create an ice
 * bridge in the direction they're moving. The ice melts after {@value BRIDGE_UP_MS}ms.
 */
public class PerkIceBridge extends Perk implements Listener {

    private static final long COOLDOWN_MS = 30_000;
    private static final long BRIDGE_UP_MS = 4_000;
    private static final String ITEM_NAME = "§aIce Bridge";

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    /** Tracks ice blocks placed by this perk so we can restore them. */
    private final List<Block> placedIce = Collections.synchronizedList(new ArrayList<>());

    public PerkIceBridge() {
        super("Ice Bridge", new String[]{
                "§7§nRight-click§r §7the §aIce §7to create an ice bridge.",
                "§7The bridge melts after §a4s§7. Cooldown: §a30s"
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ICE) return;
        if (!ITEM_NAME.equals(displayName(item))) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            long rem = (COOLDOWN_MS - (now - cooldowns.getOrDefault(player.getUniqueId(), 0L))) / 1000;
            player.sendMessage("§cIce Bridge on cooldown for §e" + rem + "s§c.");
            return;
        }

        event.setCancelled(true);
        cooldowns.put(player.getUniqueId(), now);

        // Launch player
        player.teleport(player.getLocation().add(0, 1, 0));
        player.setVelocity(player.getVelocity().add(new Vector(0, 0.5, 0)));
        player.sendMessage("§aIce Bridge activated!");

        // Build ice path
        buildIcePath(player);
    }

    private void buildIcePath(Player startPlayer) {
        new BukkitRunnable() {
            final UUID playerId = startPlayer.getUniqueId();
            final List<Block> myBlocks = new ArrayList<>();
            int ticks = 0;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(playerId);
                if (p == null || !p.isOnline() || ticks > 80) { // 4 seconds
                    scheduleRestore(myBlocks);
                    cancel();
                    return;
                }

                // Place ice below the player's feet
                Location below = p.getLocation().clone();
                below.setY(below.getY() - 1);
                Block block = below.getBlock();
                if (block.getType().isAir() || block.getType() == Material.CAVE_AIR) {
                    block.setType(Material.ICE);
                    myBlocks.add(block);
                    placedIce.add(block);
                    block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, 79);
                }

                ticks++;
            }
        }.runTaskTimer(games.sparking.altara.AltaraPaper.getPlugin(), 1L, 1L);
    }

    private void scheduleRestore(List<Block> blocks) {
        Bukkit.getScheduler().runTaskLater(games.sparking.altara.AltaraPaper.getPlugin(), () -> {
            for (Block b : blocks) {
                if (b.getType() == Material.ICE) {
                    b.setType(Material.AIR);
                }
                placedIce.remove(b);
            }
        }, BRIDGE_UP_MS / 50); // convert ms to ticks
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
        // Remove all placed ice
        for (Block b : placedIce) {
            if (b.getType() == Material.ICE) b.setType(Material.AIR);
        }
        placedIce.clear();
    }

    private static String displayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
    }
}

