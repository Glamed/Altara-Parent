package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * <b>Equip Armour</b>
 *
 * <p>Sets the player's armour slots when their kit is applied.  Any slots passed as
 * {@link Material#AIR} (or {@code null}) are left unchanged so only specific pieces
 * can be upgraded.
 *
 * <h2>Example – full iron armour</h2>
 * <pre>{@code
 * new PerkEquipArmour(
 *     Material.IRON_HELMET,
 *     Material.IRON_CHESTPLATE,
 *     Material.IRON_LEGGINGS,
 *     Material.IRON_BOOTS
 * )
 * }</pre>
 */
public class PerkEquipArmour extends Perk {

    private final Material helmet;
    private final Material chestplate;
    private final Material leggings;
    private final Material boots;

    public PerkEquipArmour(Material helmet, Material chestplate, Material leggings, Material boots) {
        super("Armour", new String[]{"§7Equipped with §a" + friendlyName(chestplate) + "§7."}, false);
        this.helmet     = helmet;
        this.chestplate = chestplate;
        this.leggings   = leggings;
        this.boots      = boots;
    }

    @Override
    public void apply(Player player) {
        var inv = player.getInventory();
        if (helmet     != null && helmet     != Material.AIR) inv.setHelmet(    new ItemStack(helmet));
        if (chestplate != null && chestplate != Material.AIR) inv.setChestplate(new ItemStack(chestplate));
        if (leggings   != null && leggings   != Material.AIR) inv.setLeggings(  new ItemStack(leggings));
        if (boots      != null && boots      != Material.AIR) inv.setBoots(     new ItemStack(boots));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static String friendlyName(Material mat) {
        if (mat == null) return "Armour";
        String raw = mat.name().toLowerCase().replace('_', ' ');
        // "iron_chestplate" → "Iron" tier
        int underscore = raw.indexOf(' ');
        return underscore > 0 ? capitalise(raw.substring(0, underscore)) : capitalise(raw);
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

