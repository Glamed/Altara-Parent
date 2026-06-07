package games.sparking.altara.chat.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import games.sparking.altara.Altara;
import lombok.Getter;
import redis.clients.jedis.JedisPooled;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single chat message delivery record stored in Redis.
 *
 * <p>One entry is created per dispatched message (keyed by {@code messageId}).
 * Remote servers that receive the same message via {@link
 * games.sparking.altara.chat.packet.ChatMessagePacket} append their own
 * viewer lists to the same key using {@link #appendRemoteViewers}.
 *
 * <h3>Redis key</h3>
 * <pre>altara:chatlog:{messageId}</pre>
 * Each key is a Redis List.  Index {@code 0} is the initial JSON payload
 * written by the origin server; subsequent entries are remote-viewer payloads
 * appended by other servers.  The entire key expires after
 * {@value #TTL_SECONDS} seconds (15 minutes).
 */
@Getter
public class ChatLogEntry {

    public static final int TTL_SECONDS = 15 * 60; // 15 minutes
    private static final String KEY_PREFIX = "altara:chatlog:";
    private static final Gson GSON = new GsonBuilder().create();

    // ── Fields (serialised to JSON) ────────────────────────────────────────────

    /** Unique ID shared across servers for the same message dispatch. */
    private final String messageId;

    /** Internal channel name (e.g. "Global", "Staff"). */
    private final String channelName;

    /** UUID of the sender as a string. */
    private final String senderUuid;

    /** Display name of the sender at the time of dispatch. */
    private final String senderName;

    /** MiniMessage-serialised form of the fully-formatted component. */
    private final String serialisedMessage;

    /** Raw message text before formatting. */
    private final String rawMessage;

    /** Epoch-millisecond timestamp of the dispatch. */
    private final long timestamp;

    /**
     * Names of players on <em>this</em> server who received the message.
     * For remote-viewer payloads this field holds the remote server's recipients.
     */
    private final List<String> viewers;

    /** Name of the server that produced this payload entry. */
    private final String serverName;

    // ── Constructor ────────────────────────────────────────────────────────────

    public ChatLogEntry(String messageId,
                        String channelName,
                        String senderUuid,
                        String senderName,
                        String serialisedMessage,
                        String rawMessage,
                        List<String> viewers,
                        String serverName) {
        this.messageId         = messageId;
        this.channelName       = channelName;
        this.senderUuid        = senderUuid;
        this.senderName        = senderName;
        this.serialisedMessage = serialisedMessage;
        this.rawMessage        = rawMessage;
        this.timestamp         = Instant.now().toEpochMilli();
        this.viewers           = viewers;
        this.serverName        = serverName;
    }

    // ── Redis operations ───────────────────────────────────────────────────────

    /**
     * Pushes this entry to the Redis list for {@code messageId} and sets (or
     * refreshes) a 15-minute TTL on the key.
     *
     * <p>Safe to call from an async thread — uses the shared {@link JedisPooled}
     * from {@link Altara}.
     */
    public void save() {
        String key  = KEY_PREFIX + messageId;
        String json = GSON.toJson(this);

        Object result = Altara.getRedisService().executeCommand(jedis -> {
            jedis.lpush(key, json);
            jedis.expire(key, TTL_SECONDS);
            return true;
        });

        if (result == null) {
            System.out.println("[ChatLog] Failed to save chat log entry for messageId: " + messageId);
        }
    }

    /**
     * Convenience method called by remote servers when they receive a
     * {@link games.sparking.altara.chat.packet.ChatMessagePacket}.  Pushes a
     * viewer-list update for {@code messageId} and refreshes the TTL.
     *
     * @param messageId          the shared message ID from the packet
     * @param channelName        channel name (from packet)
     * @param serialisedMessage  pre-formatted component (from packet)
     * @param viewers            players on this remote server that could see the message
     * @param serverName         this server's local name
     */
    public static void appendRemoteViewers(String messageId,
                                           String channelName,
                                           String serialisedMessage,
                                           List<String> viewers,
                                           String serverName) {
        ChatLogEntry entry = new ChatLogEntry(
                messageId,
                channelName,
                "remote",          // sender UUID not available on remote
                "remote",          // sender name not available on remote
                serialisedMessage,
                "",                // raw message not re-transmitted
                viewers,
                serverName
        );
        entry.save();
    }

    // ── Retrieval ──────────────────────────────────────────────────────────────

    /**
     * Returns all log payloads stored for a given {@code messageId}, or an
     * empty list if the key has expired or never existed.
     *
     * @param messageId the message UUID string
     * @return list of raw JSON payload strings (newest first — LPUSH order)
     */
    public static List<String> fetchRaw(String messageId) {
        String key = KEY_PREFIX + messageId;
        List<String> result = Altara.getRedisService().executeCommand(jedis ->
                jedis.lrange(key, 0, -1));
        return result != null ? result : new ArrayList<>();
    }
}