package games.sparking.altara.hologram.leaderboard;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single named leaderboard category (e.g. "Kills", "Wins")
 * and its ordered list of {@link LeaderboardEntry entries}.
 */
@Getter
public class LeaderboardCategory {

    private final String title;
    private final List<LeaderboardEntry> entries;

    public LeaderboardCategory(String title, List<LeaderboardEntry> entries) {
        this.title   = title;
        this.entries = Collections.unmodifiableList(entries);
    }
}

