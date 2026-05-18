package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Harden</b>
 *
 * <p>Right-click axe/sword to gain temporary Resistance and a health boost for 8 seconds.
 * Cooldown: 30 seconds.
 */
public class PerkHarden extends Perk implements Listener {

    private static final long COOLDOWN_MS = 30_000;
    private final boolean isSword;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkHarden(boolean isSword) {
        super("Harden", new String[]{"§eRight-click §7" + (isSword ? "sword" : "axe") + " to §aHarden§7. Cooldown: §a30s§7."});
        this.isSword = isSword;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        Material held = player.getInventory().getItemInMainHand().getType();
        if (isSword ? !isSword(held) : !isAxe(held)) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 8 * 20, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 2, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 8 * 20, 3, true, false, false));
        player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f);
        player.sendMessage("§6Harden §7activated!");
    }

    private static boolean isAxe(Material m) {
        return switch (m) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
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

