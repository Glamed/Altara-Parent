package games.sparking.altara.game.games.skywars.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkDirtCannon;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>KitEarth</b> — Throwable dirt cannon.
 *
 * <p>Right-click the enchanted Dirt to throw knockback projectiles at enemies.
 * Dirt refills over time.
 */
public class KitEarth extends Kit {

    public KitEarth(Game game) {
        super(game, "Earth",
                Material.DIRT,
                new String[]{
                        "§7• §aDirt Cannon §7— right-click Dirt to throw it.",
                        "§7  Knockback enemies and deal §a0.5§7 damage.",
                        "§7  Refills every §a20s §7(max §a4§7)."
                },
                new PerkDirtCannon()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SHOVEL));
        // Dirt is given by PerkDirtCannon.apply()
    }
}

