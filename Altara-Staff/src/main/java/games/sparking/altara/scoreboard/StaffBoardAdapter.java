package games.sparking.altara.scoreboard;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraLobby;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.configuration.StaffConfig;
import games.sparking.altara.configuration.defaults.ServerConfig;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.reboot.RebootService;
import games.sparking.altara.reboot.RebootTask;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.stringanimation.StringAnimation;
import games.sparking.altara.stringanimation.impl.BlinkAnimation;
import games.sparking.altara.stringanimation.impl.FadeAnimation;
import games.sparking.altara.stringanimation.impl.StaticAnimation;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scoreboard used by Altara's Staff-1 server.
 *
 * Staff receive an operational dashboard based on their team.
 * Multi-team staff rotate between their available team dashboards.
 *
 * Owners / leadership with altara.staff.board.all rotate through every
 * available dashboard.
 *
 * Current statistics are filler data until the respective services
 * are implemented.
 */
public class StaffBoardAdapter implements ScoreboardAdapter {

    private static final String TITLE_BASE = "<dark_aqua><bold>";
    private static final String TITLE_HIGHLIGHT = "<aqua><bold>";

    /**
     * The existing queue/reboot block rotates using this interval.
     */
    private static final long ROTATE_INTERVAL = 100L;

    private final AtomicReference<Component> title =
            new AtomicReference<>(Component.empty());

    private int rotateTick;

    public StaffBoardAdapter() {
        String text = config().getScoreboardTitle();

        StringAnimation animation = new StringAnimation();

        animation.add(
                new StaticAnimation(
                        TITLE_BASE + text,
                        10
                )
        );

        animation.add(
                new FadeAnimation(
                        text,
                        TITLE_BASE,
                        TITLE_HIGHLIGHT,
                        false
                )
        );

        animation.add(
                new BlinkAnimation(
                        text,
                        TITLE_BASE,
                        TITLE_HIGHLIGHT,
                        3,
                        2
                )
        );

        animation.add(
                new StaticAnimation(
                        TITLE_BASE + text,
                        10
                )
        );

        animation.add(
                new FadeAnimation(
                        text,
                        TITLE_BASE,
                        TITLE_HIGHLIGHT,
                        true
                )
        );

        animation.add(
                new BlinkAnimation(
                        text,
                        TITLE_BASE,
                        TITLE_HIGHLIGHT,
                        3,
                        2
                )
        );

        animation.whenTicked(value ->
                title.set(CC.format(value))
        );

        animation.start(4L);

        AltaraPaper.getPlugin()
                .getServer()
                .getScheduler()
                .runTaskTimer(
                        AltaraPaper.getPlugin(),
                        () -> rotateTick++,
                        ROTATE_INTERVAL,
                        ROTATE_INTERVAL
                );
    }

    private static StaffConfig config() {
        return AltaraLobby
                .getLobbyInstance()
                .getStaffConfig();
    }

    @Override
    public Component getTitle(Player player) {
        return title.get();
    }

    @Override
    public List<Component> getLines(Player player) {
        Profile profile = Altara
                .getSharedInstance()
                .getProfileService()
                .getProfile(player);

        if (profile == null) {
            return List.of();
        }

        StaffConfig config = config();
        ServerConfig server = config.getServerConfig();

        QueueService queueService =
                AltaraPaper
                        .getPaperInstance()
                        .getQueueService();

        String primaryQueue =
                queueService.getPrimaryQueue(
                        player.getUniqueId()
                );

        int online = ServerInfo.getGlobalPlayerCount();

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.parsed(
                        "rank",
                        rankDisplay(profile)
                ),

                Placeholder.unparsed(
                        "onlinecount",
                        String.valueOf(online)
                ),

                Placeholder.unparsed(
                        "maxcount",
                        String.valueOf(online + 1)
                ),

                Placeholder.unparsed(
                        "connection_address",
                        nullToEmpty(server.getIp())
                ),

                Placeholder.unparsed(
                        "store_address",
                        nullToEmpty(server.getStore())
                ),

                Placeholder.unparsed(
                        "web_address",
                        nullToEmpty(server.getWebsite())
                )
        );

        List<Component> lines = new ArrayList<>();

        for (String line : config.getScoreBoardLines()) {
            switch (line) {

                case "<rotate>" -> {
                    boolean hasQueue =
                            primaryQueue != null
                                    && !config
                                    .getScoreBoardQueueLines()
                                    .isEmpty();

                    boolean hasReboot =
                            RebootService.getRebootTask() != null
                                    && !config
                                    .getScoreBoardRebootLines()
                                    .isEmpty();

                    if (
                            hasQueue
                                    && (
                                    !hasReboot
                                            || rotateTick % 2 == 0
                            )
                    ) {
                        lines.addAll(
                                queueLines(
                                        player,
                                        queueService,
                                        primaryQueue
                                )
                        );
                    } else if (hasReboot) {
                        lines.addAll(rebootLines());
                    }
                }

                case "<queue>" ->
                        lines.addAll(
                                queueLines(
                                        player,
                                        queueService,
                                        primaryQueue
                                )
                        );

                case "<reboot>" ->
                        lines.addAll(rebootLines());

                case "<staff>" ->
                        lines.addAll(
                                staffLines(player)
                        );

                default ->
                        lines.add(
                                CC.format(
                                        line,
                                        placeholders
                                )
                        );
            }
        }

        return lines;
    }

    /*
     * ======================================================================
     * Staff-1
     * ======================================================================
     */

    private List<Component> staffLines(Player player) {
        List<StaffTeam> teams = getTeams(player);

        if (teams.isEmpty()) {
            return List.of();
        }

        StaffTeam currentTeam =
                teams.get(
                        Math.floorMod(
                                rotateTick,
                                teams.size()
                        )
                );

        List<Component> lines = new ArrayList<>();

        lines.addAll(
                commonStaffLines(currentTeam)
        );

        switch (currentTeam) {
            case SOCIAL_MEDIA ->
                    lines.addAll(socialMediaLines());

            case SUPPORT ->
                    lines.addAll(supportLines());

            case TRUST_AND_SAFETY ->
                    lines.addAll(trustSafetyLines());

            case QUALITY_ASSURANCE ->
                    lines.addAll(qualityAssuranceLines());

            case LEVEL_DESIGN ->
                    lines.addAll(levelDesignLines());

            case COMMUNITY_MANAGEMENT ->
                    lines.addAll(communityManagementLines());
        }

        lines.addAll(networkLines());

        return lines;
    }

    /*
     * ======================================================================
     * Shared Staff Information
     * ======================================================================
     */

    private static List<Component> commonStaffLines(
            StaffTeam team
    ) {
        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "staff_team",
                        team.getDisplayName()
                ),

                Placeholder.unparsed(
                        "staff_online",
                        String.valueOf(getOnlineStaffCount())
                ),

                Placeholder.unparsed(
                        "staff_server_online",
                        String.valueOf(
                                Bukkit.getOnlinePlayers().size()
                        )
                ),

                Placeholder.unparsed(
                        "network_online",
                        String.valueOf(
                                ServerInfo.getGlobalPlayerCount()
                        )
                )
        );

        return format(
                config().getScoreBoardStaffLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Social Media
     * ======================================================================
     */

    private static List<Component> socialMediaLines() {

        /*
         * TODO:
         * Replace with SocialMediaService.
         */

        int scheduledPosts = 8;
        int draftPosts = 3;
        int pendingApproval = 2;
        int activeCampaigns = 2;

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "scheduled_posts",
                        String.valueOf(scheduledPosts)
                ),

                Placeholder.unparsed(
                        "draft_posts",
                        String.valueOf(draftPosts)
                ),

                Placeholder.unparsed(
                        "pending_approval",
                        String.valueOf(pendingApproval)
                ),

                Placeholder.unparsed(
                        "active_campaigns",
                        String.valueOf(activeCampaigns)
                )
        );

        return format(
                config().getScoreBoardSocialMediaLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Support
     * ======================================================================
     */

    private static List<Component> supportLines() {

        /*
         * TODO:
         * Replace with TicketService.
         */

        int openTickets = 142;
        int unassignedTickets = 18;
        int waitingTickets = 31;
        int escalatedTickets = 4;

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "open_tickets",
                        String.valueOf(openTickets)
                ),

                Placeholder.unparsed(
                        "unassigned_tickets",
                        String.valueOf(unassignedTickets)
                ),

                Placeholder.unparsed(
                        "waiting_tickets",
                        String.valueOf(waitingTickets)
                ),

                Placeholder.unparsed(
                        "escalated_tickets",
                        String.valueOf(escalatedTickets)
                )
        );

        return format(
                config().getScoreBoardSupportLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Trust & Safety
     * ======================================================================
     */

    private static List<Component> trustSafetyLines() {

        /*
         * TODO:
         * Replace with:
         *
         * ReportService
         * InvestigationService
         * AppealService
         */

        int reports = 31;
        int unassignedReports = 7;
        int investigations = 4;
        int escalations = 2;

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "reports",
                        String.valueOf(reports)
                ),

                Placeholder.unparsed(
                        "unassigned_reports",
                        String.valueOf(unassignedReports)
                ),

                Placeholder.unparsed(
                        "investigations",
                        String.valueOf(investigations)
                ),

                Placeholder.unparsed(
                        "safety_escalations",
                        String.valueOf(escalations)
                )
        );

        return format(
                config().getScoreBoardTrustSafetyLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Quality Assurance
     * ======================================================================
     */

    private static List<Component> qualityAssuranceLines() {

        /*
         * TODO:
         * Replace with:
         *
         * QualityAssuranceService
         * DeploymentService
         */

        int openBugs = 27;
        int activeTests = 8;
        int regressions = 2;
        int awaitingQA = 6;

        String testingBuild = "#1851";

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "open_bugs",
                        String.valueOf(openBugs)
                ),

                Placeholder.unparsed(
                        "active_tests",
                        String.valueOf(activeTests)
                ),

                Placeholder.unparsed(
                        "regressions",
                        String.valueOf(regressions)
                ),

                Placeholder.unparsed(
                        "awaiting_qa",
                        String.valueOf(awaitingQA)
                ),

                Placeholder.unparsed(
                        "testing_build",
                        testingBuild
                )
        );

        return format(
                config()
                        .getScoreBoardQualityAssuranceLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Level Design
     * ======================================================================
     */

    private static List<Component> levelDesignLines() {

        /*
         * TODO:
         * Replace with MapService / LevelDesignService.
         */

        int mapsDevelopment = 11;
        int mapsAwaitingQA = 4;
        int mapsRevision = 3;
        int mapsApproved = 2;

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "maps_development",
                        String.valueOf(mapsDevelopment)
                ),

                Placeholder.unparsed(
                        "maps_awaiting_qa",
                        String.valueOf(mapsAwaitingQA)
                ),

                Placeholder.unparsed(
                        "maps_revision",
                        String.valueOf(mapsRevision)
                ),

                Placeholder.unparsed(
                        "maps_approved",
                        String.valueOf(mapsApproved)
                )
        );

        return format(
                config().getScoreBoardLevelDesignLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Community Management
     * ======================================================================
     */

    private static List<Component> communityManagementLines() {

        /*
         * TODO:
         * Replace with:
         *
         * CommunityService
         * CreatorService
         * EventService
         */

        int upcomingEvents = 3;
        int creatorRequests = 6;
        int activeCreators = 24;
        int communityProjects = 4;

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "upcoming_events",
                        String.valueOf(upcomingEvents)
                ),

                Placeholder.unparsed(
                        "creator_requests",
                        String.valueOf(creatorRequests)
                ),

                Placeholder.unparsed(
                        "active_creators",
                        String.valueOf(activeCreators)
                ),

                Placeholder.unparsed(
                        "community_projects",
                        String.valueOf(communityProjects)
                )
        );

        return format(
                config()
                        .getScoreBoardCommunityManagementLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Network
     * ======================================================================
     */

    private static List<Component> networkLines() {
        int networkOnline =
                ServerInfo.getGlobalPlayerCount();

        int staffOnline =
                getOnlineStaffCount();

        int staffServerOnline =
                Bukkit.getOnlinePlayers().size();

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "network_online",
                        formatNumber(networkOnline)
                ),

                Placeholder.unparsed(
                        "staff_online",
                        String.valueOf(staffOnline)
                ),

                Placeholder.unparsed(
                        "staff_server_online",
                        String.valueOf(staffServerOnline)
                )
        );

        return format(
                config().getScoreBoardStaffNetworkLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Staff Team Resolution
     * ======================================================================
     */

    private static List<StaffTeam> getTeams(
            Player player
    ) {
        /*
         * Owners / senior leadership can see everything.
         */
        if (
                player.hasPermission(
                        "altara.staff.board.all"
                )
        ) {
            return Arrays.asList(
                    StaffTeam.values()
            );
        }

        List<StaffTeam> teams = new ArrayList<>();

        if (
                player.hasPermission(
                        "altara.staff.board.socialmedia"
                )
        ) {
            teams.add(
                    StaffTeam.SOCIAL_MEDIA
            );
        }

        if (
                player.hasPermission(
                        "altara.staff.board.support"
                )
        ) {
            teams.add(
                    StaffTeam.SUPPORT
            );
        }

        if (
                player.hasPermission(
                        "altara.staff.board.trustandsafety"
                )
        ) {
            teams.add(
                    StaffTeam.TRUST_AND_SAFETY
            );
        }

        if (
                player.hasPermission(
                        "altara.staff.board.qa"
                )
        ) {
            teams.add(
                    StaffTeam.QUALITY_ASSURANCE
            );
        }

        if (
                player.hasPermission(
                        "altara.staff.board.leveldesign"
                )
        ) {
            teams.add(
                    StaffTeam.LEVEL_DESIGN
            );
        }

        if (
                player.hasPermission(
                        "altara.staff.board.community"
                )
        ) {
            teams.add(
                    StaffTeam.COMMUNITY_MANAGEMENT
            );
        }

        return teams;
    }

    /*
     * TODO:
     *
     * Move this into the shared Altara module later.
     * Teams should eventually be part of Profile/staff data rather
     * than inferred from permissions.
     */
    public enum StaffTeam {

        SOCIAL_MEDIA(
                "Social Media"
        ),

        SUPPORT(
                "Support"
        ),

        TRUST_AND_SAFETY(
                "Trust & Safety"
        ),

        QUALITY_ASSURANCE(
                "Quality Assurance"
        ),

        LEVEL_DESIGN(
                "Level Design"
        ),

        COMMUNITY_MANAGEMENT(
                "Community Management"
        );

        private final String displayName;

        StaffTeam(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /*
     * ======================================================================
     * Existing Queue / Reboot Functionality
     * ======================================================================
     */

    private static List<Component> queueLines(
            Player player,
            QueueService queueService,
            String queue
    ) {
        if (queue == null) {
            return List.of();
        }

        int position =
                queueService.getPosition(
                        player.getUniqueId(),
                        queue
                );

        if (position < 0) {
            return List.of();
        }

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed(
                        "queue_name",
                        queue
                ),

                Placeholder.unparsed(
                        "queue_position",
                        String.valueOf(position + 1)
                ),

                Placeholder.unparsed(
                        "queue_total",
                        String.valueOf(
                                queueService
                                        .getQueueing(queue)
                                        .size()
                        )
                )
        );

        return format(
                config().getScoreBoardQueueLines(),
                placeholders
        );
    }

    private static List<Component> rebootLines() {
        RebootTask task =
                RebootService.getRebootTask();

        if (task == null) {
            return List.of();
        }

        TagResolver placeholders =
                TagResolver.resolver(
                        Placeholder.unparsed(
                                "time_remaining",
                                Time.formatHHMMSS(
                                        task.getSecondsRemaining(),
                                        TimeUnit.SECONDS
                                )
                        )
                );

        return format(
                config().getScoreBoardRebootLines(),
                placeholders
        );
    }

    /*
     * ======================================================================
     * Rank
     * ======================================================================
     */

    /**
     * Rank display names are staff-authored MiniMessage, so they're parsed.
     * Disguised staff also see their real rank.
     */
    private static String rankDisplay(
            Profile profile
    ) {
        String shown =
                rankName(
                        profile.getCurrentGrant()
                );

        if (!profile.isDisguised()) {
            return shown;
        }

        return shown
                + " <gray>("
                + rankName(
                profile.getRealCurrentGrant()
        )
                + "<gray>)";
    }

    private static String rankName(
            Grant grant
    ) {
        return grant != null
                && grant.asRank() != null
                ? grant.asRank().getDisplayName()
                : "<gray>None";
    }

    /*
     * ======================================================================
     * Helpers
     * ======================================================================
     */

    /**
     * Currently counts local players with a staff permission.
     *
     * Once Altara has a network-wide staff presence service,
     * replace this with that service.
     */
    private static int getOnlineStaffCount() {
        return (int) Bukkit
                .getOnlinePlayers()
                .stream()
                .filter(player ->
                        player.hasPermission("altara.staff")
                                || player.hasPermission(
                                "altara.staff.board.all"
                        )
                )
                .count();
    }

    private static String formatNumber(
            int value
    ) {
        if (value >= 1_000_000) {
            return String.format(
                    Locale.US,
                    "%.1fm",
                    value / 1_000_000.0
            );
        }

        if (value >= 1_000) {
            return String.format(
                    Locale.US,
                    "%.1fk",
                    value / 1_000.0
            );
        }

        return String.valueOf(value);
    }

    /**
     * Formats configurable MiniMessage templates.
     *
     * Also retains compatibility with the older %placeholder%
     * configuration syntax.
     */
    private static List<Component> format(
            List<String> templates,
            TagResolver placeholders
    ) {
        if (templates == null || templates.isEmpty()) {
            return List.of();
        }

        List<Component> lines =
                new ArrayList<>(templates.size());

        for (String template : templates) {
            if (template == null) {
                continue;
            }

            lines.add(
                    CC.format(
                            convertLegacyPlaceholders(
                                    template
                            ),
                            placeholders
                    )
            );
        }

        return lines;
    }

    /**
     * Converts old %placeholder% syntax into MiniMessage
     * <placeholder> syntax.
     *
     * This is intentionally generic so Staff-1 placeholders
     * work without maintaining a giant regex containing every
     * possible placeholder name.
     */
    private static String convertLegacyPlaceholders(
            String input
    ) {
        return input.replaceAll(
                "%([a-zA-Z0-9_]+)%",
                "<$1>"
        );
    }

    private static String nullToEmpty(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}