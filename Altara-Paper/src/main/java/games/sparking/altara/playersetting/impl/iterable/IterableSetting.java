package games.sparking.altara.playersetting.impl.iterable;

import games.sparking.altara.playersetting.PlayerSetting;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class IterableSetting<E extends ISettingIterable> extends PlayerSetting<E> {

    private static final String ENABLED_ARROW = CC.YELLOW + CC.BOLD + "  ► ";
    private static final String DISABLED_SPACER = "    ";

    public IterableSetting(String parent, String key) {
        super(parent, key);
    }

    public abstract String getDisplayName();

    public abstract E[] getOptions();

    public abstract List<String> getDescription();

    // ✅ Changed from MaterialData to Material
    public abstract Material getMaterial();

    @Override
    public ItemStack getIcon(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(getDescription());
        lore.add(" ");

        for (E option : getOptions()) {
            if (option == get(player))
                lore.add(ENABLED_ARROW + CC.RED + option.getDisplayName());
            else
                lore.add(DISABLED_SPACER + CC.RED + option.getDisplayName());
        }

        // ✅ Changed to use Material directly
        return new ItemBuilder(getMaterial())
                .setDisplayName(getDisplayName())
                .setLore(lore)
                .build();
    }

    @Override
    public void click(Player player, ClickType clickType) {
        int mod = clickType.isLeftClick() ? 1 : -1;
        int index = get(player).ordinal() + mod;
        E[] options = getOptions();

        if (index >= options.length)
            index = 0;

        if (index < 0)
            index = options.length - 1;

        set(player, options[index]);
    }
}