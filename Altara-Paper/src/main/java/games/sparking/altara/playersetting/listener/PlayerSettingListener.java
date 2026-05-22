package games.sparking.altara.playersetting.listener;

import games.sparking.blazora.playersetting.PlayerSetting;
import games.sparking.blazora.playersetting.PlayerSettingService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSettingListener implements Listener {

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        for (PlayerSetting setting : PlayerSettingService.getAllSettings())
            setting.load(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        for (PlayerSetting setting : PlayerSettingService.getAllSettings())
            setting.remove(event.getPlayer().getUniqueId());
    }

}
