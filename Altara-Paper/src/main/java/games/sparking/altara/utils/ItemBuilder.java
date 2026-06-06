package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }

    // --- Display Name ---

    public ItemBuilder setDisplayName(Component name) {
        this.itemMeta.displayName(name);
        return this;
    }

    /**
     * Legacy fallback — translates & color codes
     */
    public ItemBuilder setDisplayName(String name) {
        this.itemMeta.displayName(CC.translateToComponent(name));
        return this;
    }

    // --- Lore ---

    public ItemBuilder setLore(List<Component> lore) {
        this.itemMeta.lore(lore);
        return this;
    }

    /**
     * Accepts both {@code List<Component>} and {@code List<String>}.
     * Strings are treated as MiniMessage format strings.
     */
    @SuppressWarnings("unchecked")
    public ItemBuilder setLoreRaw(List<?> lore) {
        if (lore == null || lore.isEmpty()) {
            this.itemMeta.lore(List.of());
            return this;
        }
        if (lore.get(0) instanceof Component) {
            this.itemMeta.lore((List<Component>) lore);
        } else {
            List<Component> components = new ArrayList<>();
            for (Object line : lore) {
                components.add(CC.translateToComponent(line.toString()));
            }
            this.itemMeta.lore(components);
        }
        return this;
    }

    public ItemBuilder setLore(Component... lore) {
        this.itemMeta.lore(Arrays.asList(lore));
        return this;
    }

    /**
     * Sets lore from plain strings. Each line is treated as a MiniMessage string.
     */
    public ItemBuilder setLore(String... lore) {
        List<Component> components = new ArrayList<>();
        for (String line : lore) {
            components.add(CC.translateToComponent(line));
        }
        this.itemMeta.lore(components);
        return this;
    }

    public ItemBuilder addToLore(Component... entries) {
        List<Component> lore = itemMeta.hasLore()
                ? new ArrayList<>(itemMeta.lore())
                : new ArrayList<>();
        lore.addAll(Arrays.asList(entries));
        itemMeta.lore(lore);
        return this;
    }

    // --- Enchantments ---

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        this.itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder storeEnchantment(Enchantment enchantment, int level) {
        if (this.itemMeta instanceof EnchantmentStorageMeta meta)
            meta.addStoredEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder setGlowing(boolean glowing) {
        if (glowing && itemMeta != null) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    // --- Flags & Attributes ---

    public ItemBuilder hideAttributes() {
        for (ItemFlag flag : ItemFlag.values()) {
            itemMeta.addItemFlags(flag);
        }
        return this;
    }

    public ItemBuilder addFlag(ItemFlag... flags) {
        this.itemMeta.addItemFlags(flags);
        return this;
    }

    // --- Misc ---

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        this.itemMeta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        if (this.itemMeta instanceof SkullMeta meta)
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        return this;
    }

    public ItemBuilder setArmorColor(Color color) {
        if (this.itemMeta instanceof LeatherArmorMeta meta)
            meta.setColor(color);
        return this;
    }

    public ItemBuilder setBookAuthor(String author) {
        if (this.itemMeta instanceof BookMeta meta)
            meta.setAuthor(author);
        return this;
    }

    public ItemBuilder setBookTitle(String title) {
        if (this.itemMeta instanceof BookMeta meta)
            meta.setTitle(title);
        return this;
    }

    public ItemBuilder setBookPages(List<String> pages) {
        if (this.itemMeta instanceof BookMeta meta)
            meta.setPages(pages);
        return this;
    }

    // --- Build ---

    public ItemStack build() {
        this.itemStack.setItemMeta(this.itemMeta);
        return this.itemStack;
    }

    @Override
    public ItemBuilder clone() {
        return new ItemBuilder(this.itemStack.clone());
    }
}