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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Defensive Stance</b>
 *
 * <p>Right-clicking with a sword enters defensive stance. While active (for 1 second),
 * incoming melee damage is reduced by 75%.
 */
public class PerkDefensiveStance extends Perk implements Listener {

    private final Map<UUID, Long> stanceTime = new ConcurrentHashMap<>();

    public PerkDefensiveStance() {
        super("Defensive Stance", new String[]{"§eRight-click §7sword to enter §aDefensive Stance§7 (1s)."});
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!isSword(player.getInventory().getItemInMainHand().getType())) return;
        stanceTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!hasPerk(victim)) return;
        Long t = stanceTime.get(victim.getUniqueId());
        if (t == null || System.currentTimeMillis() - t > 1000) return;
        // Reduce damage by 75%
        event.setDamage(event.getDamage() * 0.25);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 2f);
    }

    private static boolean isSword(Material m) {
        return switch (m) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    @Override
    public void remove(Player player) { stanceTime.remove(player.getUniqueId()); }

    @Override
    public void onUnregister() { stanceTime.clear(); }
}

