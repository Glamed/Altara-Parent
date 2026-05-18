package games.sparking.altara.game.games.skyfall.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkAeronaught;
import games.sparking.altara.game.kit.perks.PerkSlowDown;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Aeronaught Kit</b>
 * <ul>
 *   <li>Elytra</li>
 *   <li>{@link PerkSlowDown} — shift to decelerate</li>
 *   <li>{@link PerkAeronaught} — +45% damage while gliding</li>
 * </ul>
 */
public class KitAeronaught extends Kit {

    public KitAeronaught(Game game) {
        super(game,
                "Aeronaught",
                Material.ELYTRA,
                new String[]{ "§7Deal §a+45% §7damage to enemies while gliding." },
                new PerkSlowDown(),
                new PerkAeronaught()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        player.getInventory().addItem(
                new ItemStack(Material.WOODEN_SWORD),
                new ItemStack(Material.COOKED_BEEF, 3)
        );
    }
}

