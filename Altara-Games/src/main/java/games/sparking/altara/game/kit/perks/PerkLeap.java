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
 * <b>Leaper</b>
 *
 * <p>Right-click an axe to leap forward with a cooldown.
 */
public class PerkLeap extends Perk implements Listener {

    private final String abilityName;
    private final double power;
    private final double heightMax;
    private final long rechargeMs;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkLeap(String abilityName, double power, double heightMax, long rechargeMs) {
        super("Leaper", new String[]{
                "§eRight-click §7axe to §a" + abilityName + "§7. Cooldown: §a" + (rechargeMs / 1000) + "s§7."
        });
        this.abilityName = abilityName;
        this.power = power;
        this.heightMax = heightMax;
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
        Vector dir = player.getLocation().getDirection().normalize().multiply(power);
        dir.setY(Math.min(dir.getY(), heightMax));
        dir.setY(Math.max(dir.getY(), 0.2));
        player.setVelocity(dir);
        player.setFallDistance(0);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
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

