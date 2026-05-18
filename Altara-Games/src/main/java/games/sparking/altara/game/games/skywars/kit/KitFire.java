package games.sparking.altara.game.games.skywars.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkFireBurst;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>KitFire</b> — Fire burst AoE ability and permanent fire resistance.
 *
 * <p>Right-click the Blaze Rod to ignite all nearby enemies.
 * The player is immune to fire damage for the entire match.
 */
public class KitFire extends Kit {

    private static final PotionEffect FIRE_RESISTANCE =
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false);

    public KitFire(Game game) {
        super(game, "Fire",
                Material.BLAZE_ROD,
                new String[]{
                        "§7• §aFire Burst §7— right-click the Blaze Rod.",
                        "§7  Ignites nearby enemies for §a6§7 damage. Cooldown: §a30s",
                        "§7• §aPermanent Fire Resistance"
                },
                new PerkFireBurst()
        );
    }

    @Override
    public void apply(Player player) {
        super.apply(player);
        player.addPotionEffect(FIRE_RESISTANCE);
    }

    @Override
    public void remove(Player player) {
        super.remove(player);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }

    @Override
    protected void giveItems(Player player) {
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));

        ItemStack rod = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aFire Burst");
            rod.setItemMeta(meta);
        }
        player.getInventory().addItem(rod);
    }
}

