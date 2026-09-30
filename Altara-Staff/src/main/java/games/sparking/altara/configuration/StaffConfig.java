package games.sparking.altara.configuration;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.configuration.defaults.LocationConfig;
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
import java.util.logging.Level;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class StaffConfig extends LocalConfig {

    private boolean hidePlayers = false;

    /**
     * Plain text; the scoreboard animates it in the Altara colours.
     */
    private String scoreboardTitle = "Altara";

    /**
     * MiniMessage lines.  Placeholders:
     * <rank>
     * <onlinecount>
     * <maxcount>
     * <connection_address>
     * <store_address>
     * <web_address>
     *
     * Special blocks:
     * <rotate>
     * <queue>
     * <reboot>
     *
     * Kept identical to the Lobby scoreboard on purpose — Staff-1 shows the same
     * player-facing info, nothing team- or workload-specific.  See LobbyConfig.
     */
    private List<String> scoreBoardLines =
            Arrays.asList(
                    "<dark_gray><strikethrough>--------------------",
                    "<aqua>Online:",
                    " <white><onlinecount><gray>/<white><maxcount>",
                    " ",
                    "<aqua>Rank:",
                    " <rank>",
                    "",
                    "<rotate>",
                    "<gray><italic><connection_address>",
                    "<dark_gray><strikethrough>--------------------"
            );

    /**
     * Placeholders:
     * <queue_name>
     * <queue_position>
     * <queue_total>
     */
    private List<String> scoreBoardQueueLines =
            Arrays.asList(
                    "<aqua>Queue:",
                    " <white><queue_name> <gray>(#<queue_position>/<queue_total>)",
                    ""
            );

    /**
     * Placeholder:
     * <time_remaining>
     */
    private List<String> scoreBoardRebootLines =
            Arrays.asList(
                    "<red>Rebooting:",
                    " <white><time_remaining>",
                    ""
            );

    /*
     * ======================================================================
     * Server Selector
     * ======================================================================
     */

    /**
     * Rows × 9.
     */
    private int selectorSize = 45;

    /**
     * A {@link games.sparking.altara.menu.fill.FillTemplate} name.
     */
    private String selectorFiller = "ALTARA";

    private List<ServerSelectorEntry> serverSelector =
            Collections.singletonList(new ServerSelectorEntry());

    /*
     * ======================================================================
     * Locations
     * ======================================================================
     */

    private LocationConfig spawnLocation;
    private LocationConfig parkourStart;

    private List<SimpleLocationConfig> staffSignLocations =
            new ArrayList<>();

    public void addStaffSign(Location location) {
        staffSignLocations.add(
                new SimpleLocationConfig(location, true)
        );
    }

    public boolean removeStaffSignAt(Location location) {
        return staffSignLocations.removeIf(config ->
                config.getX() == location.getBlockX()
                        && config.getY() == location.getBlockY()
                        && config.getZ() == location.getBlockZ()
                        && location.getWorld() != null
                        && location.getWorld()
                        .getName()
                        .equals(config.getWorld())
        );
    }

    public void saveConfig() {
        try {
            AltaraLobby.getSharedInstance()
                    .getConfigurationService()
                    .saveConfiguration(
                            this,
                            new File(
                                    AltaraLobby.getPlugin().getDataFolder(),
                                    "config.json"
                            )
                    );
        } catch (IOException e) {
            AltaraLobby.getPlugin()
                    .getLogger()
                    .log(
                            Level.SEVERE,
                            "Failed to save the lobby config",
                            e
                    );
        }
    }
}