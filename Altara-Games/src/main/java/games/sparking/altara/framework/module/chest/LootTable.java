package games.sparking.altara.framework.module.chest;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An immutable collection of {@link LootItem}s that can generate a randomised
 * set of {@link ItemStack}s to fill a chest.
 *
 * <h3>How loot is generated</h3>
 * <ol>
 *   <li>A random number of <em>slots</em> to fill is chosen from
 *       {@code [minSlots, maxSlots]}.</li>
 *   <li>For each slot, one {@link LootItem} is drawn via weighted-random
 *       selection (higher {@link LootItem#getWeight()} → more likely).</li>
 *   <li>The selected item generates an {@link ItemStack} with a random amount
 *       inside its own {@code [minAmount, maxAmount]} range.</li>
 * </ol>
 *
 * <p>Build loot tables with the fluent {@link Builder}:
 * <pre>
 * LootTable table = LootTable.builder()
 *         .slots(3, 6)
 *         .add(LootItem.of(Material.IRON_SWORD).weight(3).build())
 *         .add(LootItem.of(Material.GOLDEN_APPLE).weight(1).build())
 *         .add(LootItem.of(Material.ARROW).amount(4, 16).weight(8).build())
 *         .build();
 * </pre>
 */
public final class LootTable {

    @Getter private final int minSlots;
    @Getter private final int maxSlots;
    private final List<LootItem> items;

    /** Pre-computed total weight for O(1) range normalisation. */
    private final double totalWeight;

    private LootTable(Builder builder) {
        this.minSlots    = builder.minSlots;
        this.maxSlots    = builder.maxSlots;
        this.items       = Collections.unmodifiableList(new ArrayList<>(builder.items));
        this.totalWeight = items.stream().mapToDouble(LootItem::getWeight).sum();
    }

    /** Returns an unmodifiable view of all possible loot items. */
    public List<LootItem> getItems() {
        return items;
    }

    // -------------------------------------------------------------------------
    // Loot generation
    // -------------------------------------------------------------------------

    /**
     * Generates a list of {@link ItemStack}s by drawing randomly from this table.
     *
     * <p>The number of stacks returned lies in {@code [minSlots, maxSlots]},
     * and items are selected by weighted probability. The same item <em>may</em>
     * appear more than once if selected in multiple draws.
     *
     * @return a mutable list of generated stacks; never {@code null}
     */
    public List<ItemStack> generate() {
        if (items.isEmpty()) return new ArrayList<>();

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int slots = minSlots == maxSlots
                ? minSlots
                : rng.nextInt(minSlots, maxSlots + 1);

        List<ItemStack> result = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            result.add(pickWeighted(rng).generate());
        }
        return result;
    }

    /** Picks one {@link LootItem} using weighted-random selection. */
    private LootItem pickWeighted(ThreadLocalRandom rng) {
        double roll = rng.nextDouble(totalWeight);
        double cumulative = 0;
        for (LootItem item : items) {
            cumulative += item.getWeight();
            if (roll < cumulative) return item;
        }
        return items.get(items.size() - 1); // fallback (floating-point safety)
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int minSlots = 3;
        private int maxSlots = 6;
        private final List<LootItem> items = new ArrayList<>();

        private Builder() {}

        /** Fixed number of item slots to fill (sets both min and max). */
        public Builder slots(int slots) {
            this.minSlots = slots;
            this.maxSlots = slots;
            return this;
        }

        /** Random number of item slots in {@code [min, max]}. */
        public Builder slots(int min, int max) {
            this.minSlots = min;
            this.maxSlots = max;
            return this;
        }

        /** Adds a {@link LootItem} to the table. */
        public Builder add(LootItem item) {
            items.add(item);
            return this;
        }

        public LootTable build() {
            if (items.isEmpty())     throw new IllegalStateException("LootTable must have at least one item");
            if (minSlots < 0)        throw new IllegalStateException("minSlots must be >= 0");
            if (maxSlots < minSlots) throw new IllegalStateException("maxSlots must be >= minSlots");
            return new LootTable(this);
        }
    }
}

