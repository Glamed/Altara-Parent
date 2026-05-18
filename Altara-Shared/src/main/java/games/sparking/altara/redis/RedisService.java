package games.sparking.altara.redis;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.redis.packet.PacketPubSub;
import games.sparking.altara.redis.subscriber.ListenerPubSub;
import games.sparking.altara.redis.subscriber.RedisListener;
import games.sparking.altara.redis.subscriber.RedisSubscriber;
import games.sparking.altara.utils.Statics;
import lombok.Getter;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisService {

    @Getter
    private static final List<RedisService> services = new ArrayList<>();
    @Getter
    private static volatile long lastExecution = -1;
    @Getter
    private static volatile long lastPacket = -1;
    @Getter
    private static volatile String lastPacketName = "N/A";
    @Getter
    private static volatile long lastError = -1;
    @Getter
    private static volatile boolean down = false;

    private final String channel;
    private final JedisPooled pool;
    private final String host;
    private final int port;
    private final String password;

    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean subscribed = false;
    private volatile Thread subscribeThread;

    public RedisService(String channel, String host, int port) {
        this(channel, host, port, null);
    }

    public RedisService(String channel, String host, int port, String password) {
        this.channel = channel;
        this.host = host;
        this.port = port;
        this.password = (password != null && !password.isBlank()) ? password : null;

        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder();

        if (this.password != null) {
            configBuilder.password(this.password);
        }

        this.pool = new JedisPooled(new HostAndPort(host, port), configBuilder.build(), poolConfig);
        services.add(this);
    }

    private Jedis openRawClient() {
        Jedis jedis = new Jedis(host, port);
        if (password != null) {
            jedis.auth(password);
        }
        return jedis;
    }

    public void publish(String channel, Packet packet) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("packet", packet.getClazz());
        jsonObject.addProperty("data", Statics.PLAIN_GSON.toJson(packet));
        lastPacketName = packet.getClass().getSimpleName();
        executeCommand((UnifiedJedis jedis) -> {
            jedis.publish(channel, jsonObject.toString());
            return null;
        }, true);
    }

    public void publish(Packet packet) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("packet", packet.getClazz());
        jsonObject.addProperty("data", Statics.PLAIN_GSON.toJson(packet));
        lastPacketName = packet.getClass().getSimpleName();
        executeCommand((UnifiedJedis jedis) -> {
            jedis.publish(channel, jsonObject.toString());
            return null;
        }, true);
    }

    public void publish(String channel, String message) {
        executeCommand((UnifiedJedis jedis) -> {
            jedis.publish(channel, message);
            return null;
        });
    }

    public void publish(String channel, JsonElement json) {
        executeCommand((UnifiedJedis jedis) -> {
            jedis.publish(channel, json.toString());
            return null;
        });
    }

    public RedisService subscribe() {
        if (subscribeThread != null) {
            return null;
        }

        subscribeThread = new Thread(() -> {
            try (Jedis client = openRawClient()) {
                subscribed = true;
                client.subscribe(new PacketPubSub(), channel);
            }
        }, "Redis Subscriber");

        subscribeThread.setDaemon(true);
        subscribeThread.start();

        return this;
    }

    public void addListener(RedisListener... listeners) {
        Arrays.stream(listeners).forEach(listener ->
                Arrays.stream(listener.getClass().getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(RedisSubscriber.class)
                                && method.getParameterCount() > 0
                                && method.getParameterCount() < 3)
                        .forEach(method -> executor.submit(() -> {
                            try (Jedis client = openRawClient()) {
                                RedisSubscriber annotation = method.getAnnotation(RedisSubscriber.class);
                                client.subscribe(new ListenerPubSub(listener, method), annotation.channels());
                            }
                        })));
    }

    public <T> T executeCommand(RedisCommand<T> command) {
        return executeCommand(command, false);
    }

    private <T> T executeCommand(RedisCommand<T> command, boolean packet) {
        if (packet) lastPacket = System.currentTimeMillis();
        else lastExecution = System.currentTimeMillis();

        try {
            down = false;
            return command.execute(pool);
        } catch (Exception e) {
            e.printStackTrace();
            lastError = System.currentTimeMillis();
            down = true;
            return null;
        }
    }
}