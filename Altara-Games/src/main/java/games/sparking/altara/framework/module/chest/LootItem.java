package games.sparking.altara.framework.module.chest;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * A single entry inside a {@link LootTable}.
 *
 * <p>Each {@code LootItem} describes:
 * <ul>
 *   <li>Which {@link Material} to give.</li>
 *   <li>A random amount range ({@code [minAmount, maxAmount]}).</li>
 *   <li>A relative {@code weight} used during weighted-random selection —
 *       higher weight means the item appears more frequently.</li>
 * </ul>
 *
 * <p>Use the {@link Builder} for a fluent construction experience:
 * <pre>
 * LootItem sword = LootItem.of(Material.IRON_SWORD)
 *         .amount(1)
 *         .weight(5)
 *         .build();
 * </pre>
 */
public final class LootItem {

    @Getter private final Material material;
    @Getter private final int minAmount;
    @Getter private final int maxAmount;
    @Getter private final double weight;

    private LootItem(Builder builder) {
        this.material  = builder.material;
        this.minAmount = builder.minAmount;
        this.maxAmount = builder.maxAmount;
        this.weight    = builder.weight;
    }

    /**
     * Generates an {@link ItemStack} with a random amount in
     * {@code [minAmount, maxAmount]}.
     */
    public ItemStack generate() {
        int amount = minAmount == maxAmount
                ? minAmount
                : minAmount + (int) (Math.random() * (maxAmount - minAmount + 1));
        return new ItemStack(material, Math.max(1, amount));
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /** Starts a builder for the given {@code material}. */
    public static Builder of(Material material) {
        return new Builder(material);
    }

    public static final class Builder {

        private final Material material;
        private int minAmount = 1;
        private int maxAmount = 1;
        private double weight = 1.0;

        private Builder(Material material) {
            this.material = material;
        }

        /** Fixed amount (sets both min and max to {@code amount}). */
        public Builder amount(int amount) {
            this.minAmount = amount;
            this.maxAmount = amount;
            return this;
        }

        /** Random amount in {@code [min, max]}. */
        public Builder amount(int min, int max) {
            this.minAmount = min;
            this.maxAmount = max;
            return this;
        }

        /**
         * Relative weight for weighted-random selection.
         * Defaults to {@code 1.0}; higher = more common.
         */
        public Builder weight(double weight) {
            this.weight = weight;
            return this;
        }

        public LootItem build() {
            if (material == null)      throw new IllegalStateException("material must not be null");
            if (minAmount < 1)         throw new IllegalStateException("minAmount must be >= 1");
            if (maxAmount < minAmount) throw new IllegalStateException("maxAmount must be >= minAmount");
            if (weight <= 0)           throw new IllegalStateException("weight must be > 0");
            return new LootItem(this);
        }
    }
}

