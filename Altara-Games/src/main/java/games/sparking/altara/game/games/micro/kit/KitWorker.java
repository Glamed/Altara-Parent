package games.sparking.altara.game.games.micro.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Worker Kit</b>
 * <ul>
 *   <li>Wood Sword, Stone Shovel, Stone Pickaxe, Stone Axe, 4 Apples</li>
 *   <li>No special perk — excels at gathering resources quickly.</li>
 * </ul>
 */
public class KitWorker extends Kit {

    public KitWorker(Game game) {
        super(game,
                "Worker",
                Material.STONE_PICKAXE,
                new String[]{ "§7Gather resources faster with stone tools." }
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.WOODEN_SWORD),
                new ItemStack(Material.STONE_SHOVEL),
                new ItemStack(Material.STONE_PICKAXE),
                new ItemStack(Material.STONE_AXE),
                new ItemStack(Material.APPLE, 4)
        );
    }
}

