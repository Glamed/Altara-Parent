package games.sparking.altara.queue;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.queue.packet.QueueLeavePacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PlayerMessagePacket;
import games.sparking.altara.uuid.UUIDUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class QueueService {

    public List<String> getQueues(UUID uuid) {
        return new ArrayList<>(Altara.getRedisService().executeCommand(redis ->
                redis.hgetAll(String.format(Queue.POSITION_FORMAT, uuid.toString())).keySet()));
    }

    public boolean isQueueingFor(UUID uuid, String server) {
        return getQueues(uuid).contains(server);
    }

    public int getPosition(UUID uuid, String server) {
        return Integer.parseInt(Altara.getRedisService().executeCommand(redis ->
                redis.hget(String.format(Queue.POSITION_FORMAT, uuid.toString()), server)));
    }

    public String getPrimaryQueue(UUID uuid) {
        List<String> queues = getQueues(uuid);
        return queues.isEmpty() ? null : queues.get(0);
    }

    public List<UUID> getQueueing(String server) {
        return Altara.getRedisService().executeCommand(redis -> {
            if (!redis.exists(String.format(Queue.PLAYERS_FORMAT, server)))
                return new ArrayList<>();

            if (redis.get(String.format(Queue.PLAYERS_FORMAT, server)).isEmpty())
                return new ArrayList<>();

            List<UUID> queueing = new ArrayList<>();
            for (String s : redis.get(String.format(Queue.PLAYERS_FORMAT, server)).split(";")) {
                if (!UUIDUtils.isUUID(s))
                    continue;

                queueing.add(UUID.fromString(s));
            }

            return queueing;
        });
    }

    public void resetQueueData(UUID uuid) {
        getQueues(uuid).forEach(server -> {
            new QueueLeavePacket(server, uuid).publish();
            Altara.getRedisService().executeCommand(redis -> {
                redis.del(String.format(Queue.POSITION_FORMAT, uuid.toString()));
                redis.hdel(String.format(Queue.WEIGHT_FORMAT, server), uuid.toString());

                if (!redis.exists(String.format(Queue.PLAYERS_FORMAT, server)))
                    return new ArrayList<>();

                if (redis.get(String.format(Queue.PLAYERS_FORMAT, server)).isEmpty())
                    return new ArrayList<>();

                List<String> inQueue = new ArrayList<>();
                for (String s : redis.get(String.format(Queue.PLAYERS_FORMAT, server)).split(";")) {
                    if (!UUIDUtils.isUUID(s))
                        continue;

                    inQueue.add(s);
                }

                inQueue.remove(uuid.toString());
                redis.set(String.format(Queue.PLAYERS_FORMAT, server), StringUtils.join(inQueue, ";"));
                inQueue.clear();
                return null;
            });
        });
    }

    public void startTask() {
        AtomicLong lastMessage = new AtomicLong(System.currentTimeMillis());
        Tasks.runTimerAsync(() -> {
            if (System.currentTimeMillis() >= lastMessage.get() + TimeUnit.SECONDS.toMillis(15)) {
                AtomicInteger position = new AtomicInteger(0);

                synchronized (AltaraPaper.getPaperInstance().getQueue().getPlayers()) {
                    AltaraPaper.getPaperInstance().getQueue().getPlayers().forEach(player ->
                            new PlayerMessagePacket(player,
                                    CC.format("&6You are position &f%d &6out of &f%d &6in the &f%s &6queue.",
                                            position.incrementAndGet(), AltaraPaper.getPaperInstance().getQueue().getPlayers().size(),
                                            AltaraPaper.getSharedInstance().getLocalServerName()),
                                    CC.translate("&7&oYou can purchase a rank at &6&o"
                                            + AltaraPaper.getPaperInstance().getLocalConfig().getServerConfig().getWebsite()
                                            + " &7&oto get a higher priority.")).publish()
                    );

                    lastMessage.set(System.currentTimeMillis());
                }
            }

            if (AltaraPaper.getPaperInstance().getLocalConfig().isQueuePaused() /*|| Bukkit.hasWhitelist()*/)
                return;

            for (int i = 0; i < AltaraPaper.getPaperInstance().getLocalConfig().getQueueRate(); i++)
                AltaraPaper.getPaperInstance().getQueue().sendNext();
        }, 20L, 20L);
    }


}
