package games.sparking.altara.game.module.chest;

import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A single item entry inside a {@link ChestLootPool}.
 *
 * <p>Package-private — instantiated only by {@link ChestLootPool}.
 * Call {@link #getItem()} to obtain a fresh, randomised clone.
 */
public class ChestLootItem
{

    private final ItemStack _item;
    private final int       _lowestAmount;
    private final int       _highestAmount;

    ChestLootItem(ItemStack item, int lowestAmount, int highestAmount)
    {
        _item          = item.clone();
        _lowestAmount  = Math.max(1, lowestAmount);
        _highestAmount = Math.max(_lowestAmount, highestAmount);
    }

    /**
     * Returns a cloned copy of this item with a randomised stack size
     * (uniformly chosen between {@code lowestAmount} and {@code highestAmount}, inclusive).
     */
    public ItemStack getItem()
    {
        ItemStack itemStack = _item.clone();

        if (_lowestAmount != _highestAmount)
        {
            itemStack.setAmount(
                    _lowestAmount + ThreadLocalRandom.current().nextInt(_highestAmount - _lowestAmount + 1));
        }

        return itemStack;
    }
}

