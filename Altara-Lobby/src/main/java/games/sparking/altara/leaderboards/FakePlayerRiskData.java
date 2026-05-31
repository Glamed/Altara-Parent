package games.sparking.altara.leaderboards;

import games.sparking.altara.hologram.leaderboard.LeaderboardEntry;

import java.util.*;

/**
 * Provides fake <b>player-risk / profiler</b> leaderboard data for testing.
 *
 * <p>Categories surface players who are flagged most often by the moderation
 * pipeline: who's been reported the most, received the most punishments,
 * racked up the highest risk score, etc.
 *
 * <p>In a real implementation, back this with your punishment / report / profiler
 * repository instead of the static maps below.
 */
public final class FakePlayerRiskData {

    // -----------------------------------------------------------------------
    // Category registry
    // -----------------------------------------------------------------------

    /** Ordered list of available category keys. */
    public static final List<String> CATEGORIES = Arrays.asList(
            "reported", "punishments", "chatflags", "alts", "riskscore", "appeals"
    );

    /** Human-readable display titles (same order as {@link #CATEGORIES}). */
    public static final List<String> TITLES = Arrays.asList(
            "Most Reported",
            "Most Punished",
            "Chat Violations",
            "Alt Accounts",
            "Risk Score",
            "Appeals Filed"
    );

    /** Unit labels shown after each score value. */
    public static final Map<String, String> UNITS;

    static {
        Map<String, String> u = new LinkedHashMap<>();
        u.put("reported",    "reports");
        u.put("punishments", "punishments");
        u.put("chatflags",   "flags");
        u.put("alts",        "alts");
        u.put("riskscore",   "pts");
        u.put("appeals",     "appeals");
        UNITS = Collections.unmodifiableMap(u);
    }

    // -----------------------------------------------------------------------
    // Fake player name pool  (suspiciously generic, as rule-breaker accounts tend to be)
    // -----------------------------------------------------------------------
    private static final String[] PLAYERS = {
            "xX_N0Sc0pe_Xx", "h4xx0r99",   "SpeedHaxLOL",  "AimBotPro",
            "InvisWallz",    "KillAura1",   "NoCooldown_",  "FlyHaxer",
            "ReachPlus3",    "AutoClicker", "ESP_Vision",   "StaffKiller",
            "BanEvader22",   "ToxicRanker", "AltFarm_"
    };

    private static final Map<String, List<LeaderboardEntry>> DATA = new LinkedHashMap<>();

    static {
        DATA.put("reported",    buildShuffled(PLAYERS, 420,  "reports"));
        DATA.put("punishments", buildShuffled(PLAYERS, 85,   "punishments"));
        DATA.put("chatflags",   buildShuffled(PLAYERS, 1_500, "flags"));
        DATA.put("alts",        buildShuffled(PLAYERS, 24,   "alts"));
        DATA.put("riskscore",   buildShuffled(PLAYERS, 9_800, "pts"));
        DATA.put("appeals",     buildShuffled(PLAYERS, 38,   "appeals"));
    }

    private FakePlayerRiskData() { }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the ranked list for a category, or an empty list if not found.
     */
    public static List<LeaderboardEntry> get(String category) {
        return DATA.getOrDefault(category.toLowerCase(), Collections.emptyList());
    }

    /** Returns the human-readable title for a category key. */
    public static String titleFor(String category) {
        int idx = CATEGORIES.indexOf(category.toLowerCase());
        return (idx >= 0) ? TITLES.get(idx) : capitalize(category);
    }

    /** Returns the unit label for a category key (e.g. {@code "reports"}). */
    public static String unitFor(String category) {
        return UNITS.getOrDefault(category.toLowerCase(), "");
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static List<LeaderboardEntry> buildShuffled(String[] names, long baseMax, String unit) {
        List<String> pool = new ArrayList<>(Arrays.asList(names));
        Collections.shuffle(pool, new Random());

        List<LeaderboardEntry> list = new ArrayList<>();
        long score = baseMax;
        for (int i = 0; i < pool.size(); i++) {
            list.add(new LeaderboardEntry(i + 1, pool.get(i), Math.max(0, score), unit));
            score -= (long) (baseMax * 0.06 + Math.random() * baseMax * 0.04);
        }
        return list;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}

