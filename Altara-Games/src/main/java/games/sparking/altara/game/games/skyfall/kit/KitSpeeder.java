package games.sparking.altara.game.games.skyfall.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkIncreaseBoosters;
import games.sparking.altara.game.kit.perks.PerkSlowDown;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Speeder Kit</b>
 * <ul>
 *   <li>Elytra</li>
 *   <li>{@link PerkSlowDown} — shift to decelerate</li>
 *   <li>{@link PerkIncreaseBoosters} — +10% ring boost multiplier</li>
 * </ul>
 */
public class KitSpeeder extends Kit {

    public KitSpeeder(Game game) {
        super(game,
                "Speeder",
                Material.PACKED_ICE,
                new String[]{ "§7Boost rings give §a+10% §7extra momentum." },
                new PerkSlowDown(),
                new PerkIncreaseBoosters()
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

