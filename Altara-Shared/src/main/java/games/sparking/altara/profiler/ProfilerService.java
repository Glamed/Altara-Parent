package games.sparking.altara.profiler;

import games.sparking.altara.Altara;
import games.sparking.altara.utils.Statics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Network-wide profiler state.
 *
 * <p>Flags, staff verifications and "recently checked" markers live in Redis hashes so
 * they survive server switches and restarts, and every entry carries an expiry that is
 * enforced lazily on read.  Each server additionally keeps a small in-memory cache of
 * the flags belonging to players currently online on it, so the chat hot-path never
 * touches Redis.
 *
 * <p>All Redis-backed methods block — call them off the main thread.
 */
public class ProfilerService {

    /** Permission node that allows a staff member to see and use profiler features. */
    public static final String PERMISSION = "altara.profiler";

    /** Score at which an account is flagged and shadow-muted. */
    public static final int FLAG_THRESHOLD = 100;

    /** How long a flag lasts if no staff member resolves it. */
    public static final long FLAG_TTL = TimeUnit.DAYS.toMillis(3);

    /** How long a staff verification protects an account from being re-flagged. */
    public static final long VERIFY_TTL = TimeUnit.DAYS.toMillis(30);

    /** Skip re-evaluating the same account + IP within this window (server switches). */
    public static final long CHECK_COOLDOWN = TimeUnit.HOURS.toMillis(6);

    private static final String FLAGS_KEY    = "profiler:flags";
    private static final String VERIFIED_KEY = "profiler:verified";
    private static final String CHECKED_KEY  = "profiler:checked";

    /** Flags of players online on this server. */
    private final Map<UUID, ProfilerRecord> online = new ConcurrentHashMap<>();

    // ── Local cache (players on this server) ──────────────────────────────────

    public void cache(ProfilerRecord record) {
        online.put(record.getUuid(), record);
    }

    public void uncache(UUID uuid) {
        online.remove(uuid);
    }

    /** Returns the cached flag of an online player, or {@code null}. */
    public ProfilerRecord getRecord(UUID uuid) {
        ProfilerRecord record = online.get(uuid);
        if (record != null && record.isExpired()) {
            online.remove(uuid);
            return null;
        }
        return record;
    }

    /** Hot-path check used by chat and private messages. */
    public boolean isShadowMuted(UUID uuid) {
        return getRecord(uuid) != null;
    }

    // ── Flags (Redis) ──────────────────────────────────────────────────────────

    public ProfilerRecord loadFlag(UUID uuid) {
        String raw = Altara.getRedisService().executeCommand(redis -> redis.hget(FLAGS_KEY, uuid.toString()));
        if (raw == null) return null;

        ProfilerRecord record = parse(raw);
        if (record == null || record.isExpired()) {
            deleteFlag(uuid);
            return null;
        }
        return record;
    }

    public void saveFlag(ProfilerRecord record) {
        String json = Statics.PLAIN_GSON.toJson(record);
        Altara.getRedisService().executeCommand(redis -> redis.hset(FLAGS_KEY, record.getUuid().toString(), json));
    }

    public void deleteFlag(UUID uuid) {
        Altara.getRedisService().executeCommand(redis -> redis.hdel(FLAGS_KEY, uuid.toString()));
    }

    /** All unexpired flags across the network, newest first. Expired entries are purged. */
    public List<ProfilerRecord> getAllFlags() {
        Map<String, String> raw = Altara.getRedisService().executeCommand(redis -> redis.hgetAll(FLAGS_KEY));
        if (raw == null) return Collections.emptyList();

        List<ProfilerRecord> records = new ArrayList<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            ProfilerRecord record = parse(entry.getValue());
            if (record == null || record.isExpired()) {
                Altara.getRedisService().executeCommand(redis -> redis.hdel(FLAGS_KEY, entry.getKey()));
                continue;
            }
            records.add(record);
        }

        records.sort(Comparator.comparingLong(ProfilerRecord::getFlaggedAt).reversed());
        return records;
    }

    // ── Verification (Redis) ───────────────────────────────────────────────────

    public boolean isVerified(UUID uuid) {
        String raw = Altara.getRedisService().executeCommand(redis -> redis.hget(VERIFIED_KEY, uuid.toString()));
        if (raw == null) return false;

        try {
            if (Long.parseLong(raw) > System.currentTimeMillis()) return true;
        } catch (NumberFormatException ignored) {
        }
        Altara.getRedisService().executeCommand(redis -> redis.hdel(VERIFIED_KEY, uuid.toString()));
        return false;
    }

    /** Clears any flag and protects the account from re-flagging for {@link #VERIFY_TTL}. */
    public void verify(UUID uuid) {
        String expiresAt = String.valueOf(System.currentTimeMillis() + VERIFY_TTL);
        Altara.getRedisService().executeCommand(redis -> redis.hset(VERIFIED_KEY, uuid.toString(), expiresAt));
        deleteFlag(uuid);
        uncache(uuid);
    }

    // ── Evaluation cooldown (Redis) ────────────────────────────────────────────

    /** True if this account was already evaluated from this IP within {@link #CHECK_COOLDOWN}. */
    public boolean wasRecentlyChecked(UUID uuid, String ip) {
        String raw = Altara.getRedisService().executeCommand(redis -> redis.hget(CHECKED_KEY, uuid.toString()));
        if (raw == null) return false;

        int split = raw.lastIndexOf('|');
        if (split < 0) return false;
        try {
            long checkedAt = Long.parseLong(raw.substring(split + 1));
            return raw.substring(0, split).equals(ip)
                    && System.currentTimeMillis() - checkedAt < CHECK_COOLDOWN;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void markChecked(UUID uuid, String ip) {
        String value = ip + "|" + System.currentTimeMillis();
        Altara.getRedisService().executeCommand(redis -> redis.hset(CHECKED_KEY, uuid.toString(), value));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static ProfilerRecord parse(String json) {
        try {
            return Statics.PLAIN_GSON.fromJson(json, ProfilerRecord.class);
        } catch (Exception e) {
            return null;
        }
    }
}
