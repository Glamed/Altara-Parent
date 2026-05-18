package games.sparking.altara.game.games.skywars.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkDoubleJump;
import games.sparking.altara.game.kit.perks.PerkNoFallDamage;
import games.sparking.altara.game.kit.perks.PerkVoidSaver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * <b>KitAir</b> — Double-jump, void saver, and no fall damage.
 *
 * <p>The Eye of Ender acts as the Void Saver trigger item.
 * The player also gets a wooden sword as a starter weapon.
 */
public class KitAir extends Kit {

    public KitAir(Game game) {
        super(game, "Air",
                Material.ENDER_EYE,
                new String[]{
                        "§7• §aLeap §7— press jump twice mid-air.",
                        "§7• §aVoid Saver §7— right-click the Eye to teleport to safety.",
                        "§7• §aNo Fall Damage"
                },
                new PerkDoubleJump(1.1, 1.2),
                new PerkVoidSaver(),
                new PerkNoFallDamage()
        );
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));

        // Give Eye of Ender (the Void Saver trigger item)
        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = eye.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aEye of Ender");
            eye.setItemMeta(meta);
        }
        player.getInventory().addItem(eye);
    }
}

