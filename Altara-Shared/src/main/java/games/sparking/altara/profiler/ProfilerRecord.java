package games.sparking.altara.profiler;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An active profiler flag for a single account.  While a record exists (and has not
 * expired) the player is shadow-muted network-wide.  Records are persisted in Redis
 * by {@link ProfilerService} and are removed when staff verify or ban the player.
 */
@Getter
@NoArgsConstructor
public class ProfilerRecord {

    private UUID uuid;
    private String name;

    /** Internal suspicion score that triggered the flag. */
    private int score;

    /** Human-readable reasons the account was flagged, shown to staff. */
    private List<String> reasons = new ArrayList<>();

    /** Linked accounts (shared IPs) that are currently banned. */
    private int bannedAltCount;

    /** The IP the flagged login came from. */
    private String ip;

    private long flaggedAt;
    private long expiresAt;

    public ProfilerRecord(UUID uuid, String name, int score, List<String> reasons,
                          int bannedAltCount, String ip, long ttlMillis) {
        this.uuid           = uuid;
        this.name           = name;
        this.score          = score;
        this.reasons        = new ArrayList<>(reasons);
        this.bannedAltCount = bannedAltCount;
        this.ip             = ip;
        this.flaggedAt      = System.currentTimeMillis();
        this.expiresAt      = flaggedAt + ttlMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
