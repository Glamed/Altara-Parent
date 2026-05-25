package games.sparking.altara.profiler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared profiler service used by every Altara module.
 *
 * <p>Maintains an in-memory registry of all flagged players.  Records are populated
 * via {@link #flag} (called by the scoring engine on join) and removed when a player
 * is verified or banned via {@link #verify} / {@code /profilerban}.
 *
 * <p>Cross-server synchronisation is handled by Redis packets in the
 * {@code games.sparking.altara.profiler.packet} package.
 */
public class ProfilerService {

    /** Permission node that allows a staff member to see and use profiler features. */
    public static final String PERMISSION = "altara.profiler";

    /** Score threshold at which an account is considered flagged / shadow-muted. */
    public static final int FLAG_THRESHOLD = 100;

    // ── Registry ───────────────────────────────────────────────────────────────

    /** Active flagged records keyed by player UUID. */
    private final Map<UUID, ProfilerRecord> records = new ConcurrentHashMap<>();

    // ── Mutation ───────────────────────────────────────────────────────────────

    /**
     * Flag a player with the given profiler score and compromised-alt count.
     * This creates a {@link ProfilerRecord} and marks them as shadow-muted.
     *
     * @return the newly created record, or the existing record if already flagged
     */
    public ProfilerRecord flag(UUID uuid, String name, int score, int compromisedAltCount) {
        ProfilerRecord existing = records.get(uuid);
        if (existing != null) return existing;

        ProfilerRecord record = new ProfilerRecord(uuid, name, score, compromisedAltCount);
        records.put(uuid, record);
        return record;
    }

    /**
     * Mark a player as verified (cleared by staff).
     * Removes their shadow mute and marks the record as verified so they are not
     * re-flagged this session.  The record stays in memory until the player leaves.
     *
     * @return {@code true} if the record existed and was updated
     */
    public boolean verify(UUID uuid) {
        ProfilerRecord record = records.get(uuid);
        if (record == null) return false;
        record.setShadowMuted(false);
        record.setVerified(true);
        return true;
    }

    /**
     * Remove a player's record entirely (called after a successful profiler ban
     * or when the player quits so we don't hold stale entries).
     */
    public void remove(UUID uuid) {
        records.remove(uuid);
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /** Returns the profiler record for a player, or {@code null} if not flagged. */
    public ProfilerRecord getRecord(UUID uuid) {
        return records.get(uuid);
    }

    /** Returns {@code true} if the player is currently shadow-muted by the profiler. */
    public boolean isShadowMuted(UUID uuid) {
        ProfilerRecord record = records.get(uuid);
        return record != null && record.isShadowMuted();
    }

    /** Returns an unmodifiable view of all currently-flagged records. */
    public Collection<ProfilerRecord> getFlaggedRecords() {
        return Collections.unmodifiableCollection(records.values());
    }

    /** Returns all flagged records that have not yet been verified. */
    public List<ProfilerRecord> getUnresolvedRecords() {
        List<ProfilerRecord> list = new ArrayList<>();
        for (ProfilerRecord record : records.values()) {
            if (!record.isVerified()) list.add(record);
        }
        return list;
    }
}


