package games.sparking.altara.command.playersetting.impl;

import games.sparking.altara.command.playersetting.PlayerSetting;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class BooleanSetting extends PlayerSetting<Boolean> {


    private static final String ENABLED_ARROW = CC.YELLOW + CC.BOLD + "  ► ";
    private static final String DISABLED_SPACER = "    ";

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
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(getDescription());
        lore.add(" ");

        Boolean state = get(player);
        lore.add((state ? ENABLED_ARROW : DISABLED_SPACER) + CC.RED + getEnabledText());
        lore.add((state ? DISABLED_SPACER : ENABLED_ARROW) + CC.RED + getDisabledText());

        return new ItemBuilder(getMaterial())
                .setDisplayName((state ? CC.GREEN : CC.RED) + CC.BOLD + getDisplayName())
                .setLore(lore)
                .build();
    }

    @Override
    public void click(Player player, ClickType clickType) {
        set(player, !get(player));
    }
}