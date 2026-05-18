package games.sparking.altara.game.games.skywars.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkIceBridge;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * <b>KitIce</b> — Build a temporary ice bridge to cross gaps.
 *
 * <p>Right-click the Ice item to launch upward and lay ice blocks underfoot.
 * The bridge melts after 4 seconds.
 */
public class KitIce extends Kit {

    public KitIce(Game game) {
        super(game, "Ice",
                Material.ICE,
                new String[]{
                        "§7• §aIce Bridge §7— right-click the Ice.",
                        "§7  Launches you up and creates an ice path.",
                        "§7  Bridge melts after §a4s§7. Cooldown: §a30s"
                },
                new PerkIceBridge()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));

        ItemStack ice = new ItemStack(Material.ICE);
        ItemMeta meta = ice.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aIce Bridge");
            ice.setItemMeta(meta);
        }
        player.getInventory().addItem(ice);
    }
}

