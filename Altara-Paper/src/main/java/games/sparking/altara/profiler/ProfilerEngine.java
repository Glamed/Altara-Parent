package games.sparking.altara.profiler;

import games.sparking.altara.profile.Profile;

/**
 * Calculates a numeric "suspicion score" for an account when it joins the network.
 *
 * <p><b>Score interpretation</b>
 * <ul>
 *   <li>{@code < 100}  – account appears normal, no action taken</li>
 *   <li>{@code ≥ 100}  – account is flagged and shadow-muted</li>
 * </ul>
 *
 * <p>The concrete criteria are intentionally not documented in player-facing resources.
 */
public class ProfilerEngine {

    /** Minimum score required to flag an account. */
    public static final int FLAG_THRESHOLD = ProfilerService.FLAG_THRESHOLD;

    // ── Scoring weights ────────────────────────────────────────────────────────

    private static final int SCORE_AGE_LESS_THAN_1_DAY  = 50;
    private static final int SCORE_AGE_LESS_THAN_7_DAYS = 25;

    private static final int SCORE_PLAYTIME_LESS_THAN_5_MIN  = 50;
    private static final int SCORE_PLAYTIME_LESS_THAN_30_MIN = 30;

    private static final int SCORE_MANY_IPS        = 40;  // > 15 known IPs
    private static final int SCORE_ELEVATED_IPS    = 20;  // > 5 known IPs

    private static final int SCORE_HIGH_DIGIT_RATIO  = 20;  // >50 % of name is digits
    private static final int SCORE_UNUSUAL_NAME_LEN  = 10;  // ≤5 or ≥14 chars

    private static final int SCORE_NO_PRIOR_HISTORY = 15;  // no punishments AND low play time

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Compute the profiler score for a loaded {@link Profile}.
     *
     * @param profile the profile of the joining player (should be fully loaded)
     * @return a non-negative integer; values ≥ {@link #FLAG_THRESHOLD} indicate a flag
     */
    public static int computeScore(Profile profile) {
        int score = 0;
        long now  = System.currentTimeMillis();

        // ── Account age ───────────────────────────────────────────────────────
        long ageMs = now - profile.getFirstLogin();
        if (ageMs < 24L * 60 * 60 * 1_000) {
            score += SCORE_AGE_LESS_THAN_1_DAY;
        } else if (ageMs < 7L * 24 * 60 * 60 * 1_000) {
            score += SCORE_AGE_LESS_THAN_7_DAYS;
        }

        // ── Play time (accumulated before this session) ────────────────────────
        long playTime = profile.getPlayTime();
        if (playTime < 5L * 60 * 1_000) {
            score += SCORE_PLAYTIME_LESS_THAN_5_MIN;
        } else if (playTime < 30L * 60 * 1_000) {
            score += SCORE_PLAYTIME_LESS_THAN_30_MIN;
        }

        // ── IP volatility ─────────────────────────────────────────────────────
        int ipCount = profile.getKnownIps().size();
        if (ipCount > 15) {
            score += SCORE_MANY_IPS;
        } else if (ipCount > 5) {
            score += SCORE_ELEVATED_IPS;
        }

        // ── Username analysis ─────────────────────────────────────────────────
        String name   = profile.getName();
        int    digits = 0;
        for (char c : name.toCharArray()) {
            if (Character.isDigit(c)) digits++;
        }
        double digitRatio = name.isEmpty() ? 0 : (double) digits / name.length();
        if (digitRatio > 0.5) {
            score += SCORE_HIGH_DIGIT_RATIO;
        }
        if (name.length() <= 5 || name.length() >= 14) {
            score += SCORE_UNUSUAL_NAME_LEN;
        }

        // ── No prior history combined with low play time (bot join pattern) ───
        if (profile.getPunishments().isEmpty() && playTime < 30L * 60 * 1_000) {
            score += SCORE_NO_PRIOR_HISTORY;
        }

        return score;
    }

    /** Returns {@code true} if the given score meets the flagging threshold. */
    public static boolean shouldFlag(int score) {
        return score >= FLAG_THRESHOLD;
    }
}

