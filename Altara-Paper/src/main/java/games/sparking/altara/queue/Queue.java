package games.sparking.altara.queue;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.queue.packet.QueueSendPlayerPacket;
import games.sparking.altara.uuid.UUIDUtils;
import org.bukkit.Bukkit;

import java.util.*;

/**
 * This server's queue.  Players are ordered by queue priority (highest first), and
 * first-come-first-served among equal priorities.  State is mirrored to Redis so other
 * servers can show positions.  All methods are synchronized: packets and the queue
 * task touch it from different threads.
 */
public class Queue {

    public static final String WEIGHT_FORMAT = "queue:%s:weight";
    public static final String PLAYERS_FORMAT = "queue:%s:players";
    public static final String POSITION_FORMAT = "queue-data:%s:position";

    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, Integer> priorities = new HashMap<>();

    private static String serverName() {
        return AltaraPaper.getPaperInstance().getLocalServerName();
    }

    public synchronized List<UUID> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized boolean contains(UUID uuid) {
        return players.contains(uuid);
    }

    /** Adds a player behind everyone with an equal or higher priority. */
    public synchronized void addPlayer(UUID uuid, int priority) {
        if (players.contains(uuid)) return;

        int position = players.size();
        for (int i = 0; i < players.size(); i++) {
            if (priorities.getOrDefault(players.get(i), 0) < priority) {
                position = i;
                break;
            }
        }

        players.add(position, uuid);
        priorities.put(uuid, priority);
        sync();
    }

    public synchronized void removePlayer(UUID uuid) {
        if (!players.remove(uuid)) return;

        priorities.remove(uuid);
        sync();
        deleteQueueData(uuid);
    }

    /** Sends the next player to this server if there's room. */
    public synchronized void sendNext() {
        if (players.isEmpty() || Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers()) return;

        UUID next = players.remove(0);
        priorities.remove(next);
        new QueueSendPlayerPacket(serverName(), next).publish();
        sync();
        deleteQueueData(next);
    }

    public synchronized void load() {
        Altara.getRedisService().executeCommand(redis -> {
            players.clear();
            String stored = redis.get(String.format(PLAYERS_FORMAT, serverName()));
            if (stored != null) {
                Arrays.stream(stored.split(";")).filter(UUIDUtils::isUUID).map(UUID::fromString).forEach(players::add);
            }

            priorities.clear();
            redis.hgetAll(String.format(WEIGHT_FORMAT, serverName()))
                    .forEach((uuid, value) -> priorities.put(UUID.fromString(uuid), Integer.valueOf(value)));
            return null;
        });
    }

    /** Writes the order, priorities and every player's position to Redis. */
    private void sync() {
        String server = serverName();
        List<String> order = players.stream().map(UUID::toString).toList();
        Map<String, String> priorityMap = new HashMap<>();
        priorities.forEach((uuid, value) -> priorityMap.put(uuid.toString(), String.valueOf(value)));

        Altara.getRedisService().executeCommand(redis -> {
            for (int i = 0; i < order.size(); i++) {
                redis.hset(String.format(POSITION_FORMAT, order.get(i)), server, String.valueOf(i));
            }
            redis.set(String.format(PLAYERS_FORMAT, server), String.join(";", order));
            redis.del(String.format(WEIGHT_FORMAT, server));
            if (!priorityMap.isEmpty()) redis.hset(String.format(WEIGHT_FORMAT, server), priorityMap);
            return null;
        });
    }

    private void deleteQueueData(UUID uuid) {
        Altara.getRedisService().executeCommand(redis ->
                redis.hdel(String.format(POSITION_FORMAT, uuid.toString()), serverName()));
    }
}
