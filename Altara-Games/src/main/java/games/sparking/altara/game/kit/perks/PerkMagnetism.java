package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Magnetism</b>
 *
 * <p>Right-click the Compass item to pull the player in your line-of-sight toward you
 * if they are wearing metal armour. Also grants +1 max health per metal armour piece worn.
 * Breaking Iron Ore drops an Iron Ingot directly.
 */
public class PerkMagnetism extends Perk implements Listener {

    private static final long COOLDOWN_MS = 15_000;
    private static final int RANGE = 12;
    private static final String ITEM_NAME = "§aMagnet";

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkMagnetism() {
        super("Magnetism", new String[]{
                "§7§nRight-click§r §7the §aMagnet §7to pull enemies wearing metal armor.",
                "§7Gain §a+1 max health §7per metal armor piece. Cooldown: §a15s"
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.COMPASS) return;
        if (!ITEM_NAME.equals(displayName(item))) return;

        // Find player in line of sight
        Player target = getPlayerInSight(player, RANGE);
        if (target == null) return;

        if (!getGame().hasPlayer(target)) return;
        var tgp = getGame().getGamePlayer(target).orElse(null);
        if (tgp == null || !tgp.isAlive()) return;

        int metalPieces = countMetalArmor(target);
        if (metalPieces == 0) {
            player.sendMessage("§cThey don't have metal armor!");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            long rem = (COOLDOWN_MS - (now - cooldowns.getOrDefault(player.getUniqueId(), 0L))) / 1000;
            player.sendMessage("§cMagnet on cooldown for §e" + rem + "s§c.");
            return;
        }

        event.setCancelled(true);
        cooldowns.put(player.getUniqueId(), now);

        // Particle line from player to target
        spawnLine(player.getEyeLocation(), target.getLocation().add(0, 1, 0));

        // Pull the target toward the player
        Vector pull = player.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize()
                .multiply(0.5 + metalPieces / 4.0);
        pull.setY(Math.max(0.6, pull.getY()));
        target.setVelocity(pull);
        target.damage(1.0, player);

        player.sendMessage("§aPulled §e" + target.getName() + "§a! (" + metalPieces + " metal pieces)");
    }

    /** Update max health based on metal armor every 20 ticks. */
    @EventHandler
    public void onSec(org.bukkit.event.entity.EntityDamageEvent event) {
        // We piggyback on an event to keep it simple — we actually do it via the perk apply/update
    }

    @Override
    public void apply(Player player) {
        updateMaxHealth(player);
    }

    /** Called periodically to update max health. */
    public void updateMaxHealth(Player player) {
        if (!hasPerk(player)) return;
        int metal = countMetalArmor(player);
        double base = 20.0;
        player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)
              .setBaseValue(base + metal);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        // Reset max health
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(20.0);
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
    }

    /** Iron Ore broken → drop Iron Ingot. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!hasPerk(event.getPlayer())) return;
        if (event.getBlock().getType() == Material.IRON_ORE ||
            event.getBlock().getType() == Material.DEEPSLATE_IRON_ORE) {
            event.setExpToDrop(0);
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().dropItemNaturally(
                    event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                    new ItemStack(Material.IRON_INGOT));
        }
    }

    private Player getPlayerInSight(Player player, int range) {
        for (int d = 1; d <= range; d++) {
            Location check = player.getEyeLocation()
                    .add(player.getLocation().getDirection().multiply(d));
            for (Player other : player.getWorld().getNearbyEntitiesByType(Player.class, check, 1.5)) {
                if (!other.equals(player)) return other;
            }
        }
        return null;
    }

    private int countMetalArmor(Player player) {
        int count = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) continue;
            Material m = piece.getType();
            if (m.name().startsWith("IRON_") || m.name().startsWith("GOLD_") ||
                m.name().startsWith("CHAINMAIL_")) {
                count++;
            }
        }
        return count;
    }

    private void spawnLine(Location from, Location to) {
        double dist = from.distance(to);
        Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
        Location cursor = from.clone();
        double walked = 0;
        while (walked <= dist) {
            cursor.getWorld().spawnParticle(Particle.FIREWORK, cursor, 1, 0, 0, 0, 0);
            cursor.add(step);
            walked += 0.5;
        }
    }

    private static String displayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
    }
}

