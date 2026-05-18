package games.sparking.altara.game.games.skyfall.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkElytraBoost;
import games.sparking.altara.game.kit.perks.PerkSlowDown;
import games.sparking.altara.game.kit.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Booster Kit</b>
 * <ul>
 *   <li>Elytra</li>
 *   <li>{@link PerkSlowDown} — shift to decelerate</li>
 *   <li>{@link PerkElytraBoost} — double-tap jump for speed burst</li>
 * </ul>
 */
public class KitBooster extends Kit {

    public KitBooster(Game game) {
        super(game,
                "Booster",
                Material.FIREWORK_ROCKET,
                new String[]{ "§7§nDouble-tap jump§r §7while gliding for a §aspeed burst§7." },
                new PerkSlowDown(),
                new PerkElytraBoost()
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

