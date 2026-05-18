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
 * <b>Hyper</b>
 *
 * <p>Right-click Sugar to gain Speed I for 4 seconds. Consumes one sugar.
 */
public class PerkHyper extends Perk implements Listener {

    private static final long COOLDOWN_MS = 3_000;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkHyper() {
        super("Hyper", new String[]{"§eRight-click §7sugar to go §aHYPER§7."});
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.SUGAR) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);
        // Consume one sugar
        var hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.AIR));
        player.updateInventory();

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 4 * 20, 0, true, true, true));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.5f);
    }

    @Override
    public void onUnregister() { cooldowns.clear(); }
}

