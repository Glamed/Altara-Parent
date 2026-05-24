package games.sparking.altara.punishment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A single restriction applied within a {@link Punishment}.
 * Duration is in <b>milliseconds</b> relative to {@link Punishment#getIssuedAt()}.
 * A value of {@code -1} denotes a permanent restriction.
 * A value of {@code 0} denotes an immediate (non-persistent) action.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RestrictionAction {

    private PunishmentType type;
    private long duration;

    // ── Factory helpers ────────────────────────────────────────────────────────

    public static RestrictionAction permanent(PunishmentType type) {
        return new RestrictionAction(type, -1L);
    }

    public static RestrictionAction temporary(PunishmentType type, long durationMs) {
        return new RestrictionAction(type, durationMs);
    }

    // ── Computed ───────────────────────────────────────────────────────────────

    public boolean isPermanent() {
        return duration == -1L;
    }

    /**
     * Returns {@code true} if this restriction has expired relative to the parent
     * punishment's {@code issuedAt} timestamp.
     */
    public boolean hasExpired(long issuedAt) {
        if (duration == -1L) return false;        // permanent — never expires
        if (duration == 0L)  return true;          // immediate — already done
        return System.currentTimeMillis() > issuedAt + duration;
    }
}

