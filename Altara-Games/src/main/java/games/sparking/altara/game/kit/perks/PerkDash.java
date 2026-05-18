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
 * <b>Dash</b>
 *
 * <p>Right-click a sword to dash forward rapidly.
 */
public class PerkDash extends Perk implements Listener {

    private final long cooldownMs;
    private final double distance;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkDash(long cooldownMs, double distance) {
        super("Dash", new String[]{
                "§eRight-click §7sword to §adash§7. Cooldown: §a" + (cooldownMs / 1000) + "s§7."
        });
        this.cooldownMs = cooldownMs;
        this.distance = distance;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!isSword(player.getInventory().getItemInMainHand().getType())) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < cooldownMs) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);
        Vector dir = player.getLocation().getDirection().normalize().multiply(distance).setY(0.1);
        player.setVelocity(dir);
        player.setFallDistance(0);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }

    private static boolean isSword(Material m) {
        return switch (m) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    @Override
    public void onUnregister() { cooldowns.clear(); }
}

