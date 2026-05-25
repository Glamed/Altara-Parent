package games.sparking.altara.punishment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents a player's current account standing based on their recent punishment history.
 *
 * <ul>
 *   <li>{@link #LOW}    – 0–1 prior offences within the lookback window</li>
 *   <li>{@link #MEDIUM} – 2–3 prior offences within the lookback window</li>
 *   <li>{@link #HIGH}   – 4 or more prior offences within the lookback window</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum AccountLevel {

    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2);

    private final String displayName;
    /** Ordinal-style weight used for comparisons. */
    private final int weight;

    /**
     * Derives the account level from a raw count of relevant punishments
     * within the history lookback window.
     */
    public static AccountLevel fromCount(int count) {
        if (count >= 4) return HIGH;
        if (count >= 2) return MEDIUM;
        return LOW;
    }

    public boolean isAtLeast(AccountLevel other) {
        return this.weight >= other.weight;
    }
}
