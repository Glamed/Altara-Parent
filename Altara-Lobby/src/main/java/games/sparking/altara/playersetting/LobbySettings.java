package games.sparking.altara.playersetting;

import games.sparking.altara.playersetting.impl.BooleanSetting;
import games.sparking.altara.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Collections;
import java.util.List;

public class LobbySettings implements PlayerSettingProvider {

    public static final BooleanSetting FLY_MODE
            = new BooleanSetting("lobby", "fly_mode") {
        @Override
        public String getDisplayName() {
            return "Fly Mode";
        }

        @Override
        public String getEnabledText() {
            return "Fly is enabled";
        }

        @Override
        public String getDisabledText() {
            return "Fly is disabled";
        }

        @Override
        public List<String> getDescription() {
            return Collections.singletonList(CC.YELLOW + "If enabled, you are able to fly around.");
        }

        @Override
        public Material getMaterial() {
            return Material.FEATHER;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }

        @Override
        public boolean canUpdate(Player player) {
            return player.hasPermission("lobby.fly");
        }

        @Override
        public void click(Player player, ClickType clickType) {
            super.click(player, clickType);
            player.setAllowFlight(get(player));
            player.setFlying(get(player));
        }
    };

    @Override
    public List<PlayerSetting> getProvidedSettings() {
        return Collections.singletonList(FLY_MODE);
    }

    @Override
    public int getPriority() {
        return 5;
    }
}