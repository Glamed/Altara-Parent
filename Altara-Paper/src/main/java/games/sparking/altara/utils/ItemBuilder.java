package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent {@link ItemStack} builder.  Display names and lore are rendered non-italic
 * unless the component explicitly asks for italics (Minecraft italicises custom
 * item text by default).
 */
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
        this.itemMeta.displayName(plain(name));
        return this;
    }

    /** Trusted MiniMessage template. */
    public ItemBuilder setDisplayName(String name) {
        return setDisplayName(CC.format(name));
    }

    // --- Lore ---

    public ItemBuilder setLore(List<Component> lore) {
        List<Component> lines = new ArrayList<>(lore.size());
        for (Component line : lore) lines.add(plain(line));
        this.itemMeta.lore(lines);
        return this;
    }

    public ItemBuilder setLore(Component... lore) {
        return setLore(Arrays.asList(lore));
    }

    /** Trusted MiniMessage template lines. */
    public ItemBuilder setLore(String... lore) {
        List<Component> components = new ArrayList<>(lore.length);
        for (String line : lore) components.add(CC.format(line));
        return setLore(components);
    }

    public ItemBuilder addToLore(Component... entries) {
        List<Component> lore = itemMeta.hasLore() ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        for (Component entry : entries) lore.add(plain(entry));
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
        itemMeta.setEnchantmentGlintOverride(glowing ? Boolean.TRUE : null);
        return this;
    }

    // --- Flags & Attributes ---

    public ItemBuilder hideAttributes() {
        itemMeta.addItemFlags(ItemFlag.values());
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

    public ItemBuilder setSkullOwner(OfflinePlayer owner) {
        if (this.itemMeta instanceof SkullMeta meta)
            meta.setOwningPlayer(owner);
        return this;
    }

    public ItemBuilder setArmorColor(Color color) {
        if (this.itemMeta instanceof LeatherArmorMeta meta)
            meta.setColor(color);
        return this;
    }

    // --- Build ---

    public ItemStack build() {
        this.itemStack.setItemMeta(this.itemMeta);
        return this.itemStack;
    }

    @Override
    public ItemBuilder clone() {
        return new ItemBuilder(build().clone());
    }

    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
