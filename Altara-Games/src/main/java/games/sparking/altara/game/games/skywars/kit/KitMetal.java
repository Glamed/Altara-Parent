package games.sparking.altara.game.games.skywars.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkMagnetism;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * <b>KitMetal</b> — Magnetic pull ability; gains bonus health from metal armour.
 *
 * <p>Right-click the Compass (Magnet) to magnetically pull enemies wearing
 * metal armour toward you. Also grants +1 max health per metal armour piece worn.
 * Breaking Iron Ore drops an Iron Ingot directly.
 */
public class KitMetal extends Kit {

    public KitMetal(Game game) {
        super(game, "Metal",
                Material.COMPASS,
                new String[]{
                        "§7• §aMagnetism §7— right-click the Magnet.",
                        "§7  Pulls enemies in metal armour toward you.",
                        "§7  Gain §a+1 max health §7per metal piece."
                },
                new PerkMagnetism()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
        player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));

        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aMagnet");
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
    }
}

