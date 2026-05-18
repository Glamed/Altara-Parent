package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Evade</b>
 *
 * <p>Right-clicking with a sword triggers a short dodge-roll sideways. While evading
 * (for 0.5s) incoming damage is negated.
 */
public class PerkEvade extends Perk implements Listener {

    private static final long EVADE_MS = 500;
    private static final long COOLDOWN_MS = 8_000;

    private final Map<UUID, Long> evadeTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkEvade() {
        super("Evade", new String[]{"§eRight-click §7sword to §adodge§7. Cooldown: §a8s§7."});
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!isSword(player.getInventory().getItemInMainHand().getType())) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);
        evadeTime.put(player.getUniqueId(), now);

        event.setCancelled(true);
        // Dodge sideways
        Vector side = player.getLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize().multiply(1.5);
        player.setVelocity(side);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.8f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasPerk(victim)) return;
        Long t = evadeTime.get(victim.getUniqueId());
        if (t == null || System.currentTimeMillis() - t > EVADE_MS) return;
        event.setCancelled(true);
        victim.sendMessage("§aEvaded!");
    }

    private static boolean isSword(Material m) {
        return switch (m) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    @Override
    public void remove(Player player) {
        evadeTime.remove(player.getUniqueId());
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() { evadeTime.clear(); cooldowns.clear(); }
}

