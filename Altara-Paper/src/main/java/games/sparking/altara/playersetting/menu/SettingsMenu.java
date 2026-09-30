package games.sparking.altara.playersetting.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.playersetting.PlayerSetting;
import games.sparking.altara.playersetting.PlayerSettingService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code /settings}: every setting the player may change, centred in rows of up to five. */
public class SettingsMenu extends Menu {

    private static final int[] ROW_STARTS = {11, 20, 29};

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Settings");
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public boolean isClickUpdate() {
        return true;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());

        List<PlayerSetting> visible = new ArrayList<>();
        for (PlayerSetting setting : PlayerSettingService.getAllSettings()) {
            if (setting.canUpdate(player)) visible.add(setting);
        }

        for (int row = 0; row < ROW_STARTS.length && row * 5 < visible.size(); row++) {
            int count = Math.min(5, visible.size() - row * 5);
            int start = ROW_STARTS[row] + (5 - count) / 2;
            for (int i = 0; i < count; i++) {
                buttons.put(start + i, new SettingButton(visible.get(row * 5 + i)));
            }
        }
        return buttons;
    }

    private static class SettingButton extends Button {

        private final PlayerSetting setting;

        SettingButton(PlayerSetting setting) {
            this.setting = setting;
        }

        @Override
        public ItemStack getItem(Player player) {
            return setting.getIcon(player);
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (setting.canUpdate(player)) setting.click(player, clickType);
        }
    }
}
