package games.sparking.altara.leaderboards;

import games.sparking.altara.hologram.leaderboard.LeaderboardEntry;

import java.util.*;

/**
 * Provides fake <b>staff</b> leaderboard data for testing / demo purposes.
 *
 * <p>Categories track staff actions: bans, mutes, kicks, warnings issued,
 * reports resolved, and hours of watch-time logged.
 *
 * <p>In a real implementation, swap {@link #get(String)} for a call to your
 * punishment / report repository.
 */
public final class FakeLeaderboardData {

    /** Available leaderboard categories. */
    public static final List<String> CATEGORIES = Arrays.asList(
            "bans", "mutes", "kicks", "warns", "reports", "watchtime"
    );

    /** Human-readable display titles matching the categories above (same order). */
    public static final List<String> TITLES = Arrays.asList(
            "Bans Issued", "Mutes Issued", "Kicks Issued",
            "Warnings Issued", "Reports Resolved", "Watch Time"
    );

    /** Units appended after the score value on each line. */
    public static final Map<String, String> UNITS;

    static {
        Map<String, String> u = new LinkedHashMap<>();
        u.put("bans",      "bans");
        u.put("mutes",     "mutes");
        u.put("kicks",     "kicks");
        u.put("warns",     "warns");
        u.put("reports",   "resolved");
        u.put("watchtime", "hrs");
        UNITS = Collections.unmodifiableMap(u);
    }

    // Shared pool of realistic-looking staff usernames
    private static final String[] STAFF = {
            "Zenith_", "Vortexed", "Harlow", "QuantumMod", "Celestia",
            "Drakkon", "Nylara", "Spectrex", "Jynx_", "Oberon",
            "Solace", "Hexaris", "Wraithyx", "Aerlith", "Stratos"
    };

    private static final Map<String, List<LeaderboardEntry>> DATA = new LinkedHashMap<>();

    static {
        // Each category uses the same staff pool but shuffled independently
        // so rankings differ between categories.
        DATA.put("bans",      buildShuffled(STAFF, 500,   "bans"));
        DATA.put("mutes",     buildShuffled(STAFF, 1_200, "mutes"));
        DATA.put("kicks",     buildShuffled(STAFF, 800,   "kicks"));
        DATA.put("warns",     buildShuffled(STAFF, 2_000, "warns"));
        DATA.put("reports",   buildShuffled(STAFF, 350,   "resolved"));
        DATA.put("watchtime", buildShuffled(STAFF, 900,   "hrs"));
    }

    private FakeLeaderboardData() { }

    /**
     * Returns the full ranked list for the given category,
     * or an empty list if the category is not recognised.
     */
    public static List<LeaderboardEntry> get(String category) {
        return DATA.getOrDefault(category.toLowerCase(), Collections.emptyList());
    }

    /** Returns the display title for a category key, or the key capitalised if not found. */
    public static String titleFor(String category) {
        int idx = CATEGORIES.indexOf(category.toLowerCase());
        return (idx >= 0) ? TITLES.get(idx) : capitalize(category);
    }

    /** Returns the unit label for a category key (e.g. "bans"), or empty string. */
    public static String unitFor(String category) {
        return UNITS.getOrDefault(category.toLowerCase(), "");
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a ranked list from a shuffled copy of the name pool.
     * Scores decrease from {@code baseMax} with a small random variance per step.
     */
    private static List<LeaderboardEntry> buildShuffled(String[] names, long baseMax) {
        return buildShuffled(names, baseMax, "");
    }

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
