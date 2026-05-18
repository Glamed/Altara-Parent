package games.sparking.altara.game.games.micro.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkFletcher;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Archer Kit</b>
 * <ul>
 *   <li>Wood Sword, Bow, 3 Apples</li>
 *   <li>{@link PerkFletcher} — gains arrows periodically</li>
 * </ul>
 */
public class KitArcher extends Kit {

    public KitArcher(Game game) {
        super(game,
                "Archer",
                Material.BOW,
                new String[]{ "§7Ranged kit with periodic arrow replenishment." },
                new PerkFletcher()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.WOODEN_SWORD),
                new ItemStack(Material.BOW),
                new ItemStack(Material.APPLE, 3)
        );
    }
}

