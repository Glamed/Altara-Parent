package games.sparking.altara.punishment;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A snapshot of a player's overall account standing derived from their recent
 * punishment history.
 *
 * <p>Standing is represented by a single {@link AccountLevel} (LOW / MEDIUM / HIGH)
 * computed across all sanction types (chat restrictions, suspensions, competitive bans)
 * within the {@link #LOOKBACK_MS history window}.
 *
 * <p>Build an instance from a player's full punishment history via
 * {@link #compute(List)}, then pass it to
 * {@link InfractionType#getRecommendedActions(AccountStatus)} to obtain
 * appropriately escalated punishment suggestions.
 */
@Getter
public class AccountStatus {

    /** Punishments older than this window are ignored when computing the level. */
    public static final long LOOKBACK_MS = TimeUnit.DAYS.toMillis(730); // ~2 years

    /** The player's unified account standing across all punishment categories. */
    private final AccountLevel level;

    public AccountStatus(AccountLevel level) {
        this.level = level;
    }

    /** An AccountStatus representing a clean account (level LOW). */
    public static final AccountStatus CLEAN = new AccountStatus(AccountLevel.LOW);

    // ── Factory ────────────────────────────────────────────────────────────────

    /**
     * Computes the account status from a player's full punishment history.
     * Only non-removed punishments issued within {@link #LOOKBACK_MS} are counted.
     * Each punishment contributes at most 1 to the total count regardless of how
     * many actions it contains.
     *
     * @param history the player's complete punishment list (may include expired records)
     */
    public static AccountStatus compute(List<Punishment> history) {
        long cutoff = System.currentTimeMillis() - LOOKBACK_MS;
        int total = 0;

        for (Punishment p : history) {
            if (!p.isActive() || p.getIssuedAt() < cutoff) continue;

            boolean counted = false;
            for (RestrictionAction action : p.getActions()) {
                switch (action.getType()) {
                    case CHAT_RESTRICTION, DISCORD_RESTRICTION,
                         SUSPENSION, COMP_GAMEPLAY -> counted = true;
                    default -> { /* WARN, REPORT – not counted toward level */ }
                }
                if (counted) break;
            }
            if (counted) total++;
        }

        return new AccountStatus(AccountLevel.fromCount(total));
    }

    @Override
    public String toString() {
        return "AccountStatus{level=" + level + "}";
    }
}
