package games.sparking.altara.queue;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.queue.packet.QueueSendPlayerPacket;
import games.sparking.altara.uuid.UUIDUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Queue {

    public static final String WEIGHT_FORMAT = "queue:%s:weight";
    public static final String PLAYERS_FORMAT = "queue:%s:players";
    public static final String POSITION_FORMAT = "queue-data:%s:position";

    @Getter
    private final LinkedList<UUID> players = new LinkedList<>();
    private final ConcurrentHashMap<UUID, Integer> weight = new ConcurrentHashMap<>();

    public int getJoinPosition(Profile profile) {
        if (players.isEmpty())
            return -1;

        int playerWeight = profile.getQueuePriority(Altara.getSharedInstance().getServerGroup());
        if (playerWeight == 0)
            return -1;

        for (int i = 0; i < players.size(); i++) {
            Integer otherWeight = weight.get(players.get(i));
            if (playerWeight >= otherWeight)
                return playerWeight > otherWeight ? i : i + 1;
        }

        return -1;
    }

    public void addPlayer(Profile profile) {
        if (players.contains(profile.getUuid()))
            return;
        int position = getJoinPosition(profile);
        if (position == -1)
            players.addLast(profile.getUuid());
        else players.add(position, profile.getUuid());

        weight.put(profile.getUuid(), profile.getRealCurrentGrantOn(Altara.getSharedInstance().getServerGroup()).asRank().getWeight()
                + (profile.hasPrimeStatus() ? 1 : 0));
        updatePositions();
    }

    public void removePlayer(Profile profile) {
        if (!players.contains(profile.getUuid()))
            return;

        players.remove(profile.getUuid());
        weight.remove(profile.getUuid());
        updatePositions();
        deleteQueueData(profile.getUuid());
    }

    public void save() {
        Altara.getRedisService().executeCommand(redis -> {
            List<String> strings = new ArrayList<>();
            for (UUID player : players) {
                strings.add(player.toString());
            }

            redis.set(String.format(PLAYERS_FORMAT, AltaraPaper.getPaperInstance().getLocalServerName()), StringUtils.join(strings, ";"));
            strings.clear();
            weight.forEach((uuid, value) -> redis.hset(
                    String.format(WEIGHT_FORMAT, AltaraPaper.getPaperInstance().getLocalServerName()), uuid.toString(), String.valueOf(value))
            );
            return null;
        });
    }

    public void load() {
        Altara.getRedisService().executeCommand(redis -> {
            players.clear();
            if (redis.exists(String.format(PLAYERS_FORMAT, AltaraPaper.getPaperInstance().getLocalServerName()))) {
                players.addAll(Arrays.stream(redis.get(String.format(PLAYERS_FORMAT, AltaraPaper.getPaperInstance().getLocalServerName())).split(";"))
                        .filter(UUIDUtils::isUUID)
                        .map(UUID::fromString)
                        .toList());
            }

            weight.clear();
            redis.hgetAll(String.format(WEIGHT_FORMAT, AltaraPaper.getPaperInstance().getLocalServerName()))
                    .forEach((uuid, value) -> weight.put(UUID.fromString(uuid), Integer.valueOf(value)));
            //players.addAll(weight.keySet());
            //players.sort(Comparator.comparingInt(weight::get));
            return null;
        });
    }

    private void updatePositions() {
        Altara.getRedisService().executeCommand(redis -> {
            for (UUID uuid : players) {
                if (uuid == null)
                    continue;

                redis.hset(String.format(POSITION_FORMAT, uuid.toString()), AltaraPaper.getPaperInstance().getLocalServerName(),
                        String.valueOf(players.indexOf(uuid)));
            }


            List<String> strings = new ArrayList<>();
            for (UUID player : players) {
                strings.add(player.toString());
            }

            redis.set(String.format(PLAYERS_FORMAT, AltaraPaper.getPaperInstance().getLocalServerName()), StringUtils.join(strings, ";"));
            strings.clear();
            return null;
        });
        save();
    }

    private void deleteQueueData(UUID uuid) {
        Altara.getRedisService().executeCommand(redis -> {
            redis.hdel(String.format(POSITION_FORMAT, uuid.toString()), AltaraPaper.getPaperInstance().getLocalServerName());
            return null;
        });
    }

    public void sendNext() {
        if (players.isEmpty())
            return;

        if (Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers())
            return;

        UUID next = players.removeFirst();
        if (next == null)
            return;

        new QueueSendPlayerPacket(AltaraPaper.getPaperInstance().getLocalServerName(), next).publish();
        updatePositions();
        deleteQueueData(next);
    }

}
