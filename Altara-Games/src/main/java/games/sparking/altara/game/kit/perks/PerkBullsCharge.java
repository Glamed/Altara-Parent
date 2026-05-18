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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Bull's Charge</b>
 *
 * <p>Right-click an axe to gain a burst of Speed II for 6 seconds.
 */
public class PerkBullsCharge extends Perk implements Listener {

    private static final long COOLDOWN_MS = 12_000;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkBullsCharge() {
        super("Bull's Charge", new String[]{"§eRight-click §7axe to §aBull's Charge§7. Cooldown: §a12s§7."});
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!isAxe(player.getInventory().getItemInMainHand().getType())) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6 * 20, 1, true, false, false));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5f, 0f);
        player.sendMessage("§6Bull's Charge §7activated!");
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

