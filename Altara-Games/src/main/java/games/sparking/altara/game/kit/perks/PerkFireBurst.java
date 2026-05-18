package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Fire Burst</b>
 *
 * <p>Right-click Blaze Rod to ignite all nearby enemies within {@value RANGE} blocks,
 * dealing {@value DAMAGE} damage and setting them on fire. Cooldown: varies by upgrade level.
 */
public class PerkFireBurst extends Perk implements Listener {

    private static final double RANGE = 6.0;
    private static final double DAMAGE = 6.0;
    private static final int FIRE_TICKS = 100; // 5 seconds
    private static final long COOLDOWN_MS = 30_000;

    private static final String ITEM_NAME = "§aFire Burst";

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkFireBurst() {
        super("Fire Burst", new String[]{
                "§7§nRight-click§r §7the §aBlaze Rod §7to ignite nearby enemies.",
                "§7Range: §a" + (int) RANGE + " blocks §7| Cooldown: §a30s"
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BLAZE_ROD) return;
        if (!ITEM_NAME.equals(displayName(item))) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - cooldowns.getOrDefault(player.getUniqueId(), 0L))) / 1000;
            player.sendMessage("§cFire Burst on cooldown for §e" + remaining + "s§c.");
            return;
        }

        event.setCancelled(true);
        cooldowns.put(player.getUniqueId(), now);

        Location origin = player.getLocation().add(0, 1, 0);

        // Damage and ignite nearby enemies
        for (Player target : player.getWorld().getNearbyEntities(origin, RANGE, RANGE, RANGE, e -> e instanceof Player).stream()
                .map(e -> (Player) e).toList()) {
            if (target.equals(player)) continue;
            if (!getGame().hasPlayer(target)) continue;
            var tgp = getGame().getGamePlayer(target).orElse(null);
            if (tgp == null || !tgp.isAlive()) continue;

            target.setFireTicks(FIRE_TICKS);
            target.damage(DAMAGE, player);
        }

        // Particle ring
        for (double radius = 0; radius < RANGE; radius += 0.5) {
            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                double x = radius * Math.cos(theta);
                double z = radius * Math.sin(theta);
                origin.getWorld().spawnParticle(Particle.FLAME, origin.clone().add(x, 0, z), 1, 0, 0, 0, 0.01);
            }
        }

        player.getWorld().playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
        player.sendMessage("§aFire Burst activated!");
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
    }

    private static String displayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
    }
}

