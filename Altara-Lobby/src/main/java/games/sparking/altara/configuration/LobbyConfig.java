package games.sparking.altara.configuration;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.configuration.defaults.SimpleLocationConfig;
import games.sparking.altara.selector.ServerSelectorEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class LobbyConfig extends LocalConfig {

    private boolean hidePlayers = false;

    private String scoreboardTitle = "Sparking Games";

    private List<String> scoreBoardLines =
            Arrays.asList(
                    "<dark_blue><gray><strikethrough>--------------------",
                    "<dark_red>Online:",
                    " <white><onlinecount> / <maxcount>",
                    " ",
                    "<dark_red>Rank:",
                    " <rank>",
                    " ",
                    "<rotate%",
                    "<gray><italic><connection_address>",
                    "<gray><strikethrough>--------------------");

    private List<String> scoreBoardQueueLines =
            Arrays.asList(
                    "<dark_red>Queue:",
                    " %queue_name% <gray>(#<queue_position> / <queue_total>)",
                    ""
            );

    private List<String> scoreBoardRebootLines =
            Arrays.asList(
                    "<dark_red><bold>Rebooting<gray>: <red><time_remaining>",
                    ""
            );

    private int selectorSize = 45;
    private String selectorFiller = "BORDER";
    private List<ServerSelectorEntry> serverSelector = Collections.singletonList(new ServerSelectorEntry());

    private LocationConfig spawnLocation;
    private LocationConfig parkourStart;

    private List<SimpleLocationConfig> staffSignLocations = new ArrayList<>();

    public void addStaffSign(Location location) {
        staffSignLocations.add(new SimpleLocationConfig(location, true));
    }

    public boolean removeStaffSignAt(Location location) {
        return staffSignLocations.removeIf(config -> config.getX() == location.getBlockX()
                && config.getY() == location.getBlockY()
                && config.getZ() == location.getBlockZ()
                && config.getWorld().equals(location.getWorld().getName()));
    }

    public void saveConfig() {
        try {
            AltaraLobby.getSharedInstance().getConfigurationService().saveConfiguration(this,
                    new File(AltaraLobby.getPlugin().getDataFolder(), "config.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
