package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Particle;
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
 * <b>Smoke Bomb</b>
 *
 * <p>Right-click the activator item to throw a smoke bomb at your feet, granting
 * temporary invisibility and slowing nearby enemies.
 */
public class PerkSmokebomb extends Perk implements Listener {

    private final Material activator;
    private final int durationTicks;
    private final boolean consumed;
    private static final long COOLDOWN_MS = 20_000;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkSmokebomb(Material activator, int durationTicks, boolean consumed) {
        super("Smoke Bomb", new String[]{"§eRight-click §a" + activator.name().toLowerCase() + " §7for a §aSmoke Bomb§7."});
        this.activator = activator;
        this.durationTicks = durationTicks;
        this.consumed = consumed;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (player.getInventory().getItemInMainHand().getType() != activator) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);
        if (consumed) {
            var item = player.getInventory().getItemInMainHand();
            if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
            else player.getInventory().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.AIR));
        }

        // Smoke particles
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 0.5f, 2f);

        // Cloak self
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, true, false, false));

        // Slow nearby enemies
        for (Player nearby : player.getWorld().getNearbyEntitiesByType(Player.class, player.getLocation(), 4.0)) {
            if (nearby.equals(player)) continue;
            if (!getGame().hasPlayer(nearby)) continue;
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1, true, false, false));
        }
    }

    @Override
    public void onUnregister() { cooldowns.clear(); }
}

