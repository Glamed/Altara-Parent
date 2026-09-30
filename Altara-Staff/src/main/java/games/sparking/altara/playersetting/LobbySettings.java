package games.sparking.altara.playersetting;

import games.sparking.altara.playersetting.impl.BooleanSetting;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;

public class LobbySettings implements PlayerSettingProvider {

    public static final BooleanSetting FLY_MODE = new BooleanSetting("lobby", "fly_mode") {
        @Override public String getDisplayName() { return "Lobby Flight"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("You can fly around the lobby.")
                    : List.of("Flight is off in the lobby.");
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
        return List.of(FLY_MODE);
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
