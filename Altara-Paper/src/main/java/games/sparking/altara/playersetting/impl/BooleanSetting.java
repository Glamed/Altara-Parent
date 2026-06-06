package games.sparking.altara.playersetting.impl;

import games.sparking.altara.playersetting.PlayerSetting;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BooleanSetting extends PlayerSetting<Boolean> {

    private static final Component ENABLED_ARROW = Component.text("  ► ", NamedTextColor.YELLOW, TextDecoration.BOLD);
    private static final Component DISABLED_SPACER = Component.text("    ");

    public BooleanSetting(String parent, String key) {
        super(parent, key);
    }

    public abstract String getDisplayName();
    public abstract String getEnabledText();
    public abstract String getDisabledText();
    public abstract List<String> getDescription();
    public abstract Material getMaterial();

    @Override
    public Boolean parse(String input) {
        return Boolean.parseBoolean(input);
    }

    @Override
    public ItemStack getIcon(Player player) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (String desc : getDescription())
            lore.add(Component.text(desc));
        lore.add(Component.empty());

        Boolean state = get(player);
        lore.add((state ? ENABLED_ARROW : DISABLED_SPACER).append(Component.text(getEnabledText(), CC.RED)));
        lore.add((state ? DISABLED_SPACER : ENABLED_ARROW).append(Component.text(getDisabledText(), CC.RED)));

        return new ItemBuilder(getMaterial())
                .setDisplayName(Component.text(getDisplayName(), state ? NamedTextColor.GREEN : CC.RED, TextDecoration.BOLD))
                .setLore(lore)
                .build();
    }

    @Override
    public void click(Player player, ClickType clickType) {
        set(player, !get(player));
    }
}