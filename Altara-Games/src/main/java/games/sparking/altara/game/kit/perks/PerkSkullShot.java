package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Skull Shot</b>
 *
 * <p>Right-click with a skull in hand to launch it as a projectile.
 */
public class PerkSkullShot extends Perk implements Listener {

    private static final long COOLDOWN_MS = 4000L;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkSkullShot() {
        super("Skull Shot", new String[]{
                "§7Right-click with a §8skull§7 to launch it as a projectile.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isSkull(item)) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);
        event.setCancelled(true);

        org.bukkit.entity.WitherSkull skull = player.getWorld().spawn(
                player.getEyeLocation(), org.bukkit.entity.WitherSkull.class);
        skull.setShooter(player);
        skull.setCharged(false);
        skull.setVelocity(player.getLocation().getDirection().multiply(2.0));
    }

    private boolean isSkull(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.SKELETON_SKULL
                || item.getType() == Material.WITHER_SKELETON_SKULL
                || item.getType() == Material.ZOMBIE_HEAD
                || item.getType() == Material.PLAYER_HEAD
                || item.getType() == Material.CREEPER_HEAD
                || item.getType() == Material.DRAGON_HEAD;
    }
}


