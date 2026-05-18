package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Void Saver</b>
 *
 * <p>Tracks the player's last safe ground location. Right-clicking the Eye of Ender
 * teleports the player back to that location — consuming the item.
 */
public class PerkVoidSaver extends Perk implements Listener {

    private final Map<UUID, Location> safeLocations = new ConcurrentHashMap<>();

    public PerkVoidSaver() {
        super("Void Saver", new String[]{
                "§7Tracks your §alast safe location§7.",
                "§7§nRight-click§r §7the §aEye of Ender §7to teleport back."
        });
    }

    @Override
    public void apply(Player player) {
        safeLocations.put(player.getUniqueId(), player.getLocation());
    }

    @Override
    public void remove(Player player) {
        safeLocations.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() {
        safeLocations.clear();
    }

    // Track grounded location
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (player.isOnGround()) {
            safeLocations.put(player.getUniqueId(), player.getLocation().clone());
        }
    }

    // Trigger teleport on right-click
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ENDER_EYE) return;

        Location safe = safeLocations.get(player.getUniqueId());
        if (safe == null) {
            player.sendMessage("§cNo safe location recorded yet.");
            return;
        }

        event.setCancelled(true);

        // Consume the item
        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        // Teleport
        Location dest = safe.clone().add(0, 2, 0);
        player.teleport(dest);
        player.setFallDistance(0);
        player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.getWorld().spawnParticle(Particle.WITCH, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.05);
        player.sendMessage("§aYou teleported to your safe location.");
    }
}

