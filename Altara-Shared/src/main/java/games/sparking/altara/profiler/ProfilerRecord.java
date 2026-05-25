package games.sparking.altara.profiler;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Holds the profiler state for a single player account.
 *
 * <p>A record is created the first time the profiler engine assigns a non-zero score.
 * {@link #shadowMuted} is true whenever the player has been flagged and not yet resolved.
 */
@Getter
@Setter
public class ProfilerRecord {

    private final UUID uuid;
    private final String name;

    /** Internal profiler score that triggered the flag. Higher = more suspicious. */
    private final int score;

    /**
     * Number of alt accounts (shared-IP accounts) associated with this player
     * that have themselves been banned or previously flagged.
     */
    private int compromisedAltCount;

    /** Whether this player is currently shadow-muted by the profiler. */
    private boolean shadowMuted;

    /**
     * True once a staff member has used /profilerverify on this player.
     * Verified players are no longer shadow-muted and won't be re-flagged this session.
     */
    private boolean verified;

    /** Epoch-ms when the flag was first set. */
    private final long flaggedAt;

    public ProfilerRecord(UUID uuid, String name, int score, int compromisedAltCount) {
        this.uuid               = uuid;
        this.name               = name;
        this.score              = score;
        this.compromisedAltCount = compromisedAltCount;
        this.shadowMuted        = true;
        this.verified           = false;
        this.flaggedAt          = System.currentTimeMillis();
    }
}

