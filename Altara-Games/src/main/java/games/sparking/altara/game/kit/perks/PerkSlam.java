package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Slam</b>
 *
 * <p>Right-click an axe to knock all nearby players upward and outward.
 */
public class PerkSlam extends Perk implements Listener {

    private final String abilityName;
    private final double power;
    private final double radius;
    private final long rechargeMs;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkSlam(String abilityName, double power, double radius, long rechargeMs) {
        super("Slam", new String[]{"§eRight-click §7axe to §a" + abilityName + "§7. Cooldown: §a" + (rechargeMs / 1000) + "s§7."});
        this.abilityName = abilityName;
        this.power = power;
        this.radius = radius;
        this.rechargeMs = rechargeMs;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!isAxe(player.getInventory().getItemInMainHand().getType())) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < rechargeMs) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);

        for (Player nearby : player.getWorld().getNearbyEntitiesByType(Player.class, player.getLocation(), radius)) {
            if (nearby.equals(player)) continue;
            if (!getGame().hasPlayer(nearby)) continue;
            Vector dir = nearby.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
            nearby.setVelocity(dir.multiply(power).setY(0.5));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.8f);
        player.sendMessage("§6" + abilityName + "§7!");
    }

    private static boolean isAxe(Material m) {
        return switch (m) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }

    @Override
    public void onUnregister() { cooldowns.clear(); }
}

