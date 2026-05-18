package games.sparking.altara.game.games.skyfall.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkElytraKnockback;
import games.sparking.altara.game.kit.perks.PerkSlowDown;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Jouster Kit</b>
 * <ul>
 *   <li>Elytra, Sword</li>
 *   <li>{@link PerkSlowDown} — shift to decelerate</li>
 *   <li>{@link PerkElytraKnockback} — +100% knockback while gliding</li>
 * </ul>
 */
public class KitJouster extends Kit {

    public KitJouster(Game game) {
        super(game,
                "Jouster",
                Material.IRON_SWORD,
                new String[]{ "§7Hitting players while gliding deals §a+100% knockback§7." },
                new PerkSlowDown(),
                new PerkElytraKnockback()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        player.getInventory().addItem(
                new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.COOKED_BEEF, 3)
        );
    }
}

