package games.sparking.altara.game.module.chest;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * A weighted pool of {@link ChestLootItem}s that can be sampled when filling a chest.
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * new ChestLootPool()
 *         .addItem(new ItemStack(Material.IRON_SWORD))
 *         .addEnchantment(Enchantment.SHARPNESS, 1)
 *         .setEnchantmentRarity(0.5)   // 50% chance the enchantment is applied
 *         .setProbability(0.8)         // 80% chance this pool contributes at all
 *         .setAmountsPerChest(1, 2)    // pick 1–2 items from the pool per chest
 * }</pre>
 */
public class ChestLootPool {

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    private final List<ChestLootItem>       items         = new ArrayList<>();
    private final Map<Enchantment, Integer> enchantments  = new LinkedHashMap<>();

    private double enchantRarity       = 1.0;
    private double probability         = 1.0;
    private int    minAmountsPerChest  = 1;
    private int    maxAmountsPerChest  = 1;

    // -------------------------------------------------------------------------
    // Builder – items
    // -------------------------------------------------------------------------

    /**
     * Adds an item with default weight (100) and stack size 1.
     */
    public ChestLootPool addItem(ItemStack item) {
        return addItem(item, 100, 1, 1);
    }

    /**
     * Adds an item with a custom weight and stack size 1.
     *
     * @param item   item template
     * @param weight relative selection weight (higher = more common)
     */
    public ChestLootPool addItem(ItemStack item, int weight) {
        return addItem(item, weight, 1, 1);
    }

    /**
     * Adds an item with default weight (100) and a randomised stack size.
     *
     * @param item      item template
     * @param minAmount minimum stack size
     * @param maxAmount maximum stack size (inclusive)
     */
    public ChestLootPool addItem(ItemStack item, int minAmount, int maxAmount) {
        return addItem(item, 100, minAmount, maxAmount);
    }

    /**
     * Adds an item with full control over weight and stack size.
     *
     * @param item      item template
     * @param minAmount minimum stack size
     * @param maxAmount maximum stack size (inclusive)
     * @param weight    relative selection weight
     */
    public ChestLootPool addItem(ItemStack item, int minAmount, int maxAmount, int weight) {
        items.add(new ChestLootItem(item, weight, minAmount, maxAmount));
        return this;
    }

    // -------------------------------------------------------------------------
    // Builder – enchantments
    // -------------------------------------------------------------------------

    /**
     * Adds an enchantment that <em>may</em> be applied to generated items.
     * Use {@link #setEnchantmentRarity} to control how often it is applied.
     */
    public ChestLootPool addEnchantment(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
        return this;
    }

    /**
     * Probability (0–1) that the pool's enchantments are applied to a generated item.
     * Default: {@code 1.0} (always).
     */
    public ChestLootPool setEnchantmentRarity(double rarity) {
        this.enchantRarity = rarity;
        return this;
    }

    // -------------------------------------------------------------------------
    // Builder – pool behaviour
    // -------------------------------------------------------------------------

    /**
     * Probability (0–1) that this pool contributes to a chest at all.
     * Default: {@code 1.0} (always contributes).
     */
    public ChestLootPool setProbability(double probability) {
        this.probability = probability;
        return this;
    }

    /**
     * How many distinct items are drawn from this pool per chest.
     * Default: exactly {@code 1}.
     */
    public ChestLootPool setAmountsPerChest(int min, int max) {
        this.minAmountsPerChest = Math.max(1, min);
        this.maxAmountsPerChest = Math.max(this.minAmountsPerChest, max);
        return this;
    }

    // -------------------------------------------------------------------------
    // Generation
    // -------------------------------------------------------------------------

    /**
     * Generates zero or more {@link ItemStack}s from this pool.
     *
     * <p>Returns an empty list if the pool's {@link #probability} roll fails or
     * the pool contains no items.
     *
     * @param rng random source
     * @return list of generated items (may be empty)
     */
    public List<ItemStack> generate(Random rng) {
        if (items.isEmpty()) return List.of();
        if (rng.nextDouble() >= probability) return List.of();

        int count = (minAmountsPerChest == maxAmountsPerChest)
                ? minAmountsPerChest
                : minAmountsPerChest + rng.nextInt(maxAmountsPerChest - minAmountsPerChest + 1);

        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ChestLootItem picked = weightedRandom(rng);
            if (picked == null) continue;

            // bake enchantments into the item before generating
            picked.withEnchantments(enchantments, enchantRarity);
            result.add(picked.generate(rng));
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private ChestLootItem weightedRandom(Random rng) {
        int totalWeight = items.stream().mapToInt(ChestLootItem::getWeight).sum();
        if (totalWeight <= 0) return items.get(rng.nextInt(items.size()));

        int roll = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (ChestLootItem item : items) {
            cumulative += item.getWeight();
            if (roll < cumulative) return item;
        }
        return items.get(items.size() - 1); // fallback (shouldn't happen)
    }
}

