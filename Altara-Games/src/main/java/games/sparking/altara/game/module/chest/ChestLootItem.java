package games.sparking.altara.game.module.chest;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Random;

/**
 * A single entry in a {@link ChestLootPool}.
 *
 * <p>Each item has a <em>weight</em> (relative probability during weighted-random
 * selection), a min/max stack size, and optional enchantments to apply.
 */
public class ChestLootItem {

    private final ItemStack baseItem;
    private final int       weight;
    private final int       minAmount;
    private final int       maxAmount;

    private Map<Enchantment, Integer> enchantments  = Map.of();
    private double                    enchantRarity  = 1.0;

    /**
     * @param baseItem  template – cloned on every call to {@link #generate}
     * @param weight    relative weight for weighted random selection (higher = more common)
     * @param minAmount minimum stack size
     * @param maxAmount maximum stack size (inclusive)
     */
    public ChestLootItem(ItemStack baseItem, int weight, int minAmount, int maxAmount) {
        this.baseItem  = baseItem.clone();
        this.weight    = Math.max(1, weight);
        this.minAmount = Math.max(1, minAmount);
        this.maxAmount = Math.max(this.minAmount, maxAmount);
    }

    /** Configures optional enchantments that <em>may</em> be applied. */
    ChestLootItem withEnchantments(Map<Enchantment, Integer> enchantments, double rarity) {
        this.enchantments = Map.copyOf(enchantments);
        this.enchantRarity = rarity;
        return this;
    }

    /** @return this item's relative weight */
    public int getWeight() { return weight; }

    /**
     * Generates a ready-to-give {@link ItemStack} with a randomised amount and optional
     * enchantments applied according to {@link #enchantRarity}.
     *
     * @param rng random source
     * @return a fresh clone of the base item
     */
    public ItemStack generate(Random rng) {
        ItemStack stack = baseItem.clone();

        // randomise stack size
        int amount = (minAmount == maxAmount)
                ? minAmount
                : minAmount + rng.nextInt(maxAmount - minAmount + 1);
        stack.setAmount(amount);

        // apply enchantments
        if (!enchantments.isEmpty() && rng.nextDouble() < enchantRarity) {
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta esm) {
                // enchanted book: use stored enchantments
                enchantments.forEach((e, lvl) -> esm.addStoredEnchant(e, lvl, true));
                stack.setItemMeta(esm);
            } else if (meta != null) {
                enchantments.forEach((e, lvl) -> meta.addEnchant(e, lvl, true));
                stack.setItemMeta(meta);
            }
        }

        return stack;
    }
}

