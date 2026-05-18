package games.sparking.altara.game.games.micro.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkIronSkin;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Fighter Kit</b>
 * <ul>
 *   <li>Wood Sword, 5 Apples</li>
 *   <li>{@link PerkIronSkin} — reduced incoming damage</li>
 * </ul>
 */
public class KitFighter extends Kit {

    public KitFighter(Game game) {
        super(game,
                "Fighter",
                Material.IRON_SWORD,
                new String[]{ "§7Melee kit with reduced incoming damage." },
                new PerkIronSkin()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(
                new ItemStack(Material.WOODEN_SWORD),
                new ItemStack(Material.APPLE, 5)
        );
    }
}

