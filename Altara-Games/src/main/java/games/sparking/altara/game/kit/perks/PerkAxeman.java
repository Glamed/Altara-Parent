package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * <b>Axe Master</b>
 *
 * <p>Deals +1 bonus damage when attacking with an axe.
 */
public class PerkAxeman extends Perk implements Listener {

    public PerkAxeman() {
        super("Axe Master", new String[]{"§7Deals §a+1 §7damage with axes."});
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!hasPerk(damager)) return;
        Material hand = damager.getInventory().getItemInMainHand().getType();
        if (isAxe(hand)) event.setDamage(event.getDamage() + 1.0);
    }

    private static boolean isAxe(Material m) {
        return m == Material.WOODEN_AXE || m == Material.STONE_AXE || m == Material.IRON_AXE
                || m == Material.GOLDEN_AXE || m == Material.DIAMOND_AXE || m == Material.NETHERITE_AXE;
    }
}

