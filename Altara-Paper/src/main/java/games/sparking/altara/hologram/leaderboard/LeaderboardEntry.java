package games.sparking.altara.hologram.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LeaderboardEntry {

    private final int rank;
    private final String playerName;
    private final long score;
    /** Optional unit label shown after the score, e.g. "bans" or "hrs". May be empty. */
    private final String unit;

    /** Convenience constructor — no unit label. */
    public LeaderboardEntry(int rank, String playerName, long score) {
        this(rank, playerName, score, "");
    }

}

