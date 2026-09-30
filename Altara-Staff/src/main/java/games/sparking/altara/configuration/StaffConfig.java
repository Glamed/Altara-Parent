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
     * MiniMessage lines.
     *
     * Placeholders:
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
     * <staff>
     */
    private List<String> scoreBoardLines =
            Arrays.asList(
                    "<dark_gray><strikethrough>--------------------",
                    "<aqua>Online:",
                    " <white><onlinecount><gray>/<white><maxcount>",
                    " ",
                    "<aqua>Rank:",
                    " <rank>",
                    "<rotate>",
                    "<staff>",
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
     * Staff-1 Scoreboard
     * ======================================================================
     */

    /**
     * Shared Staff-1 information.
     *
     * Placeholders:
     * <staff_team>
     * <staff_online>
     * <staff_server_online>
     * <network_online>
     */
    private List<String> scoreBoardStaffLines =
            Arrays.asList(
                    "",
                    "<dark_aqua><bold>Staff",
                    " <gray>Team: <white><staff_team>",
                    " <gray>Online: <white><staff_online>"
            );

    /**
     * Social Media.
     *
     * Placeholders:
     * <scheduled_posts>
     * <draft_posts>
     * <pending_approval>
     * <active_campaigns>
     */
    private List<String> scoreBoardSocialMediaLines =
            Arrays.asList(
                    "",
                    "<aqua>Social Media:",
                    " <gray>Scheduled: <white><scheduled_posts>",
                    " <gray>Drafts: <white><draft_posts>",
                    " <gray>Approval: <white><pending_approval>",
                    " <gray>Campaigns: <white><active_campaigns>"
            );

    /**
     * Support.
     *
     * Placeholders:
     * <open_tickets>
     * <unassigned_tickets>
     * <waiting_tickets>
     * <escalated_tickets>
     */
    private List<String> scoreBoardSupportLines =
            Arrays.asList(
                    "",
                    "<aqua>Support:",
                    " <gray>Open: <white><open_tickets>",
                    " <gray>Unassigned: <white><unassigned_tickets>",
                    " <gray>Waiting: <white><waiting_tickets>",
                    " <gray>Escalated: <white><escalated_tickets>"
            );

    /**
     * Trust & Safety.
     *
     * Placeholders:
     * <reports>
     * <unassigned_reports>
     * <investigations>
     * <safety_escalations>
     */
    private List<String> scoreBoardTrustSafetyLines =
            Arrays.asList(
                    "",
                    "<aqua>Trust & Safety:",
                    " <gray>Reports: <white><reports>",
                    " <gray>Unassigned: <white><unassigned_reports>",
                    " <gray>Investigations: <white><investigations>",
                    " <gray>Escalated: <white><safety_escalations>"
            );

    /**
     * Quality Assurance.
     *
     * Placeholders:
     * <open_bugs>
     * <active_tests>
     * <regressions>
     * <awaiting_qa>
     * <testing_build>
     */
    private List<String> scoreBoardQualityAssuranceLines =
            Arrays.asList(
                    "",
                    "<aqua>Quality Assurance:",
                    " <gray>Open Bugs: <white><open_bugs>",
                    " <gray>Testing: <white><active_tests>",
                    " <gray>Regressions: <white><regressions>",
                    " <gray>Awaiting QA: <white><awaiting_qa>",
                    " <gray>Build: <white><testing_build>"
            );

    /**
     * Level Design.
     *
     * Placeholders:
     * <maps_development>
     * <maps_awaiting_qa>
     * <maps_revision>
     * <maps_approved>
     */
    private List<String> scoreBoardLevelDesignLines =
            Arrays.asList(
                    "",
                    "<aqua>Level Design:",
                    " <gray>Development: <white><maps_development>",
                    " <gray>Awaiting QA: <white><maps_awaiting_qa>",
                    " <gray>Revisions: <white><maps_revision>",
                    " <gray>Approved: <white><maps_approved>"
            );

    /**
     * Community Management.
     *
     * Covers creators, YouTubers, events and other community-facing
     * programs.
     *
     * Placeholders:
     * <upcoming_events>
     * <creator_requests>
     * <active_creators>
     * <community_projects>
     */
    private List<String> scoreBoardCommunityManagementLines =
            Arrays.asList(
                    "",
                    "<aqua>Community:",
                    " <gray>Events: <white><upcoming_events>",
                    " <gray>Creator Requests: <white><creator_requests>",
                    " <gray>Creators: <white><active_creators>",
                    " <gray>Projects: <white><community_projects>"
            );

    /**
     * Shared network block shown after the team-specific information.
     *
     * Placeholders:
     * <network_online>
     * <staff_online>
     * <staff_server_online>
     */
    private List<String> scoreBoardStaffNetworkLines =
            Arrays.asList(
                    "",
                    "<aqua>Network:",
                    " <gray>Players: <white><network_online>",
                    " <gray>Staff: <white><staff_online>",
                    " <gray>Staff-1: <white><staff_server_online>"
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