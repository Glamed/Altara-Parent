package games.sparking.altara.game.module.chest;

import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A weighted pool of {@link ChestLootItem}s that can be sampled when filling a chest.
 *
 * <p>Matches Mineplex's {@code ChestLootPool} behaviour exactly:
 * <ul>
 *   <li>Items are selected via weighted random draw.</li>
 *   <li>One enchantment is selected via weighted random draw and applied with
 *       a random level between 1 and {@code maxLevel} (inclusive).</li>
 *   <li>The pool only contributes to a chest if its {@link #setProbability probability} roll passes.</li>
 * </ul>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * new ChestLootPool()
 *         .addItem(new ItemStack(Material.IRON_SWORD))
 *         .addEnchantment(Enchantment.SHARPNESS, 2)
 *         .setEnchantmentRarity(0.5)   // 50 % chance the enchantment is applied
 *         .setProbability(0.8)         // 80 % chance this pool contributes at all
 *         .setAmountsPerChest(1, 2)    // pick 1–2 items from the pool per chest
 * }</pre>
 */
public class ChestLootPool
{

    private static final int DEFAULT_RARITY = 100;

    // ─── Internal records ────────────────────────────────────────────────────

    private record ItemEntry(ChestLootItem item, int weight) {}

    private record EnchEntry(Enchantment enchantment, int maxLevel, int weight) {}

    // ─── State ───────────────────────────────────────────────────────────────

    private final List<ItemEntry>  _items        = new ArrayList<>();
    private final List<EnchEntry>  _enchantments = new ArrayList<>();

    private int    _minimumPerChest   = 1;
    private int    _maximumPerChest   = 1;
    private double _rarity            = 1.0;   // pool probability
    private double _enchantmentRarity = 0.0;   // enchantment application probability
    private boolean _unbreakable      = false;

    // ─── Builder ─ items ─────────────────────────────────────────────────────

    /** Adds an item with default weight (100) and the item's current stack size. */
    public ChestLootPool addItem(ItemStack itemStack)
    {
        return addItem(itemStack, itemStack.getAmount(), itemStack.getAmount(), DEFAULT_RARITY);
    }

    /** Adds an item with a custom rarity weight and the item's current stack size. */
    public ChestLootPool addItem(ItemStack itemStack, int rarity)
    {
        return addItem(itemStack, itemStack.getAmount(), itemStack.getAmount(), rarity);
    }

    /** Adds an item with default weight (100) and a randomised stack size. */
    public ChestLootPool addItem(ItemStack itemStack, int lowestAmount, int highestAmount)
    {
        return addItem(itemStack, lowestAmount, highestAmount, DEFAULT_RARITY);
    }

    /** Adds an item with full control over stack size and rarity weight. */
    public ChestLootPool addItem(ItemStack itemStack, int lowestAmount, int highestAmount, int rarity)
    {
        _items.add(new ItemEntry(new ChestLootItem(itemStack, lowestAmount, highestAmount), Math.max(1, rarity)));
        return this;
    }

    // ─── Builder ─ enchantments ──────────────────────────────────────────────

    /**
     * Adds an enchantment to the pool's enchantment table with default weight.
     * When applied, the enchantment level is chosen randomly between 1 and {@code maxLevel} inclusive.
     */
    public ChestLootPool addEnchantment(Enchantment enchantment, int maxLevel)
    {
        return addEnchantment(enchantment, maxLevel, DEFAULT_RARITY);
    }

    /**
     * Adds an enchantment to the pool's enchantment table with a custom rarity weight.
     * When applied, the enchantment level is chosen randomly between 1 and {@code maxLevel} inclusive.
     */
    public ChestLootPool addEnchantment(Enchantment enchantment, int maxLevel, int rarity)
    {
        _enchantments.add(new EnchEntry(enchantment, Math.max(1, maxLevel), Math.max(1, rarity)));
        return this;
    }

    /**
     * Sets the probability (0–1) that this pool's selected enchantment is applied to the generated item.
     * Default: {@code 0.0} (never — you must call this to enable enchanting).
     */
    public ChestLootPool setEnchantmentRarity(double probability)
    {
        _enchantmentRarity = probability;
        return this;
    }

    // ─── Builder ─ pool behaviour ────────────────────────────────────────────

    /**
     * Sets how many items this pool places into a chest.
     * A random value between {@code minimumPerChest} and {@code maximumPerChest} (inclusive) is chosen.
     * Default: exactly 1.
     */
    public ChestLootPool setAmountsPerChest(int minimumPerChest, int maximumPerChest)
    {
        _minimumPerChest = Math.max(1, minimumPerChest);
        _maximumPerChest = Math.max(_minimumPerChest, maximumPerChest);
        return this;
    }

    /**
     * Sets the probability (0–1) that this pool contributes to a chest at all.
     * Default: {@code 1.0} (always contributes).
     */
    public ChestLootPool setProbability(double probability)
    {
        _rarity = probability;
        return this;
    }

    /**
     * When {@code true}, items generated from this pool will have their durability locked
     * (unbreakable NBT tag set) if their material has durability.
     */
    public ChestLootPool setUnbreakable(boolean unbreakable)
    {
        _unbreakable = unbreakable;
        return this;
    }

    /** @return the pool's spawn probability (0–1). */
    public double getProbability()
    {
        return _rarity;
    }

    // ─── Generation ──────────────────────────────────────────────────────────

    /**
     * Places items from this pool into random slots of a chest inventory.
     * Mirrors Mineplex's {@code populateChest} exactly.
     *
     * @param chest the chest block state to populate
     * @param slots mutable list of free inventory slot indices; consumed as items are placed
     */
    public void populateChest(Chest chest, List<Integer> slots)
    {
        Inventory inventory = chest.getBlockInventory();

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int count = (_minimumPerChest == _maximumPerChest)
                ? _minimumPerChest
                : _minimumPerChest + rng.nextInt(_maximumPerChest - _minimumPerChest + 1);

        for (int i = 0; i < count && !slots.isEmpty(); i++)
        {
            int slotIdx = rng.nextInt(slots.size());
            int slot    = slots.remove(slotIdx);
            ItemStack item = getRandomItem();
            if (item != null)
            {
                inventory.setItem(slot, item);
            }
        }

        chest.update();
    }

    /**
     * Draws a single random item from this pool (weighted), optionally enchants it,
     * and optionally marks it unbreakable.
     *
     * @return a ready-to-place {@link ItemStack}, or {@code null} if the pool is empty
     */
    public ItemStack getRandomItem()
    {
        if (_items.isEmpty()) return null;

        // ── Weighted item selection ──────────────────────────────────────────
        ItemEntry picked = weightedRandom(_items, ItemEntry::weight);
        if (picked == null) return null;

        ItemStack stack = picked.item().getItem();

        // ── Weighted enchantment selection ────────────────────────────────────
        if (!_enchantments.isEmpty() && ThreadLocalRandom.current().nextDouble() < _enchantmentRarity)
        {
            EnchEntry enchEntry = weightedRandom(_enchantments, EnchEntry::weight);
            if (enchEntry != null)
            {
                // Level is random from 1..maxLevel (Mineplex: UtilMath.r(maxLevel) + 1)
                int level = enchEntry.maxLevel() == 1
                        ? 1
                        : 1 + ThreadLocalRandom.current().nextInt(enchEntry.maxLevel());
                stack.addUnsafeEnchantment(enchEntry.enchantment(), level);
            }
        }

        // ── Unbreakable ───────────────────────────────────────────────────────
        if (_unbreakable && stack.getType().getMaxDurability() > 0)
        {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null)
            {
                meta.setUnbreakable(true);
                stack.setItemMeta(meta);
            }
        }

        return stack;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /** Picks one element from {@code list} using its weight, or {@code null} if the list is empty. */
    private <T> T weightedRandom(List<T> list, java.util.function.ToIntFunction<T> weightFn)
    {
        if (list.isEmpty()) return null;

        int total = 0;
        for (T t : list) total += weightFn.applyAsInt(t);

        if (total <= 0) return list.get(0);

        int roll       = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (T t : list)
        {
            cumulative += weightFn.applyAsInt(t);
            if (roll < cumulative) return t;
        }
        return list.get(list.size() - 1); // unreachable, but safe fallback
    }
}

