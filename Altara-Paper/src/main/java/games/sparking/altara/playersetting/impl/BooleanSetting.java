package games.sparking.altara.playersetting.impl;

import games.sparking.altara.menu.Gui;
import games.sparking.altara.playersetting.PlayerSetting;
import games.sparking.altara.utils.ItemBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * An on/off setting, shown as {@code Name (Enabled)} on a lime dye or
 * {@code Name (Disabled)} on a gray dye.
 */
public abstract class BooleanSetting extends PlayerSetting<Boolean> {

    public BooleanSetting(String parent, String key) {
        super(parent, key);
    }

    public abstract String getDisplayName();

    /** Short gray lore lines describing the current state, e.g. "You can receive private messages." */
    public abstract List<String> getDescription(boolean enabled);

    @Override
    public Boolean parse(String input) {
        return Boolean.parseBoolean(input);
    }

    @Override
    public ItemStack getIcon(Player player) {
        boolean enabled = get(player);
        Gui.Lore lore = Gui.lore();
        getDescription(enabled).forEach(lore::text);

        return new ItemBuilder(Gui.settingMaterial(enabled))
                .setDisplayName(Gui.settingName(getDisplayName(), enabled))
                .setLore(lore.cta(enabled ? "disable" : "enable").build())
                .build();
    }

    @Override
    public void click(Player player, ClickType clickType) {
        set(player, !get(player));
    }
}
