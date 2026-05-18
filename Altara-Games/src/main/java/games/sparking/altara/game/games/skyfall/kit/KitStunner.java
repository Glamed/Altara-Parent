package games.sparking.altara.game.games.skyfall.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkRemoveElytra;
import games.sparking.altara.game.kit.perks.PerkSlowDown;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Stunner Kit</b>
 * <ul>
 *   <li>Elytra, Sword</li>
 *   <li>{@link PerkSlowDown} — shift to decelerate</li>
 *   <li>{@link PerkRemoveElytra} — disable enemy elytra for 1s on hit</li>
 * </ul>
 */
public class KitStunner extends Kit {

    public KitStunner(Game game) {
        super(game,
                "Stunner",
                Material.STICK,
                new String[]{ "§7Hitting gliding enemies §adisables their elytra §7for §a1s§7." },
                new PerkSlowDown(),
                new PerkRemoveElytra()
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

