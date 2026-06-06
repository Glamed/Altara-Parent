package games.sparking.altara.playersetting.impl.iterable;

import games.sparking.altara.playersetting.PlayerSetting;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class IterableSetting<E extends ISettingIterable> extends PlayerSetting<E> {

    private static final Component ENABLED_ARROW = Component.text("  ► ", CC.YELLOW, TextDecoration.BOLD);
    private static final Component DISABLED_SPACER = Component.text("    ");

    public IterableSetting(String parent, String key) {
        super(parent, key);
    }

    public abstract String getDisplayName();
    public abstract E[] getOptions();
    public abstract List<String> getDescription();
    public abstract Material getMaterial();

    @Override
    public ItemStack getIcon(Player player) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        for (String desc : getDescription())
            lore.add(Component.text(desc));
        lore.add(Component.empty());

        for (E option : getOptions()) {
            if (option == get(player))
                lore.add(ENABLED_ARROW.append(Component.text(option.getDisplayName(), CC.RED)));
            else
                lore.add(DISABLED_SPACER.append(Component.text(option.getDisplayName(), CC.RED)));
        }

        return new ItemBuilder(getMaterial())
                .setDisplayName(Component.text(getDisplayName()))
                .setLore(lore)
                .build();
    }

    @Override
    public void click(Player player, ClickType clickType) {
        int mod = clickType.isLeftClick() ? 1 : -1;
        int index = get(player).ordinal() + mod;
        E[] options = getOptions();
        if (index >= options.length) index = 0;
        if (index < 0) index = options.length - 1;
        set(player, options[index]);
    }
}