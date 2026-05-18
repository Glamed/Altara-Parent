package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Looter</b>
 *
 * <p>On kill, collect a portion of the victim's dropped items automatically.
 */
public class PerkLooter extends Perk implements Listener {

    public PerkLooter() {
        super("Looter", new String[]{
                "§7Automatically collect §adrop items§7 from players you kill."
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (!hasPerk(killer)) return;
        if (!getGame().hasPlayer(killer)) return;

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack drop : event.getDrops()) {
            if (!killer.getInventory().addItem(drop).isEmpty()) {
                remaining.add(drop); // couldn't fit, leave it
            }
        }
        event.getDrops().clear();
        event.getDrops().addAll(remaining);
    }
}

