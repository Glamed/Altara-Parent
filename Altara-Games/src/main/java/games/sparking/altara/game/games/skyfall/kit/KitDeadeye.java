package games.sparking.altara.game.games.skyfall.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkDeadeye;
import games.sparking.altara.game.kit.perks.PerkSlowDown;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Deadeye Kit</b>
 * <ul>
 *   <li>Elytra, Bow, 16 Arrows</li>
 *   <li>{@link PerkSlowDown} — shift to decelerate</li>
 *   <li>{@link PerkDeadeye} — arrows home toward nearby gliding enemies</li>
 * </ul>
 */
public class KitDeadeye extends Kit {

    public KitDeadeye(Game game) {
        super(game,
                "Deadeye",
                Material.BOW,
                new String[]{ "§7Shot arrows §ahome toward §7nearby gliding enemies." },
                new PerkSlowDown(),
                new PerkDeadeye()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        player.getInventory().addItem(
                new ItemStack(Material.WOODEN_SWORD),
                new ItemStack(Material.BOW),
                new ItemStack(Material.ARROW, 16),
                new ItemStack(Material.COOKED_BEEF, 3)
        );
    }
}

