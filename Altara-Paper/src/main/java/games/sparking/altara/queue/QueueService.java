package games.sparking.altara.queue;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.queue.packet.QueueLeavePacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PlayerMessagePacket;
import games.sparking.altara.uuid.UUIDUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Network-wide queue lookups (positions live in Redis) and this server's queue task.
 * Lookups are cached for a second — scoreboards and holograms call them every few ticks.
 */
public class QueueService {

    private static final long CACHE_MILLIS = 1000L;
    private static final long POSITION_MESSAGE_INTERVAL = TimeUnit.SECONDS.toMillis(15);

    private record Cached<T>(T value, long at) {
        boolean fresh() {
            return System.currentTimeMillis() - at < CACHE_MILLIS;
        }
    }

    private final Map<UUID, Cached<Map<String, String>>> positions = new ConcurrentHashMap<>();
    private final Map<String, Cached<List<UUID>>> queueing = new ConcurrentHashMap<>();

    private static <K, T> T cached(Map<K, Cached<T>> cache, K key, Supplier<T> loader) {
        Cached<T> entry = cache.get(key);
        if (entry != null && entry.fresh()) return entry.value();

        T value = loader.get();
        cache.put(key, new Cached<>(value, System.currentTimeMillis()));
        return value;
    }

    /** server → 0-based position for every queue the player is in. */
    private Map<String, String> positionsOf(UUID uuid) {
        return cached(positions, uuid, () -> {
            Map<String, String> result = Altara.getRedisService().executeCommand(redis ->
                    redis.hgetAll(String.format(Queue.POSITION_FORMAT, uuid.toString())));
            return result == null ? Map.of() : result;
        });
    }

    public List<String> getQueues(UUID uuid) {
        return new ArrayList<>(positionsOf(uuid).keySet());
    }

    public boolean isQueueingFor(UUID uuid, String server) {
        return positionsOf(uuid).containsKey(server);
    }

    /** 0-based position, or {@code -1} if the player isn't queued for {@code server}. */
    public int getPosition(UUID uuid, String server) {
        String value = positionsOf(uuid).get(server);
        try {
            return value == null ? -1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getPrimaryQueue(UUID uuid) {
        List<String> queues = getQueues(uuid);
        return queues.isEmpty() ? null : queues.get(0);
    }

    public List<UUID> getQueueing(String server) {
        return cached(queueing, server, () -> {
            String stored = Altara.getRedisService().executeCommand(redis -> redis.get(String.format(Queue.PLAYERS_FORMAT, server)));
            if (stored == null || stored.isEmpty()) return List.of();

            List<UUID> result = new ArrayList<>();
            for (String s : stored.split(";")) {
                if (UUIDUtils.isUUID(s)) result.add(UUID.fromString(s));
            }
            return result;
        });
    }

    /** Removes a player from every queue on the network. */
    public void resetQueueData(UUID uuid) {
        for (String server : getQueues(uuid)) {
            new QueueLeavePacket(server, uuid).publish();
        }
        Altara.getRedisService().executeCommand(redis -> redis.del(String.format(Queue.POSITION_FORMAT, uuid.toString())));
        positions.remove(uuid);
    }

    /** Sends queued players in and reminds them of their position every 15 seconds. */
    public void startTask() {
        long[] lastMessage = {System.currentTimeMillis()};
        Tasks.runTimerAsync(() -> {
            Queue queue = AltaraPaper.getPaperInstance().getQueue();

            if (System.currentTimeMillis() - lastMessage[0] >= POSITION_MESSAGE_INTERVAL) {
                lastMessage[0] = System.currentTimeMillis();
                List<UUID> players = queue.getPlayers();
                String server = AltaraPaper.getSharedInstance().getLocalServerName();
                String store = AltaraPaper.getPaperInstance().getMainConfig().getServerConfig().getStore();

                for (int i = 0; i < players.size(); i++) {
                    new PlayerMessagePacket(players.get(i),
                            CC.notice("Queue position.", "You're *" + (i + 1) + "* of *" + players.size() + "* in the *" + server + "* queue."),
                            CC.info("Ranks move you up the queue. Visit *" + store + "* to get one.")).publish();
                }
            }

            if (AltaraPaper.getPaperInstance().getLocalConfig().isQueuePaused()) return;

            for (int i = 0; i < AltaraPaper.getPaperInstance().getLocalConfig().getQueueRate(); i++) {
                queue.sendNext();
            }
        }, 20L, 20L);
    }
}
