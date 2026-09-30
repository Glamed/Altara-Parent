package games.sparking.altara.uuid;

import games.sparking.altara.Altara;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.uuid.packets.UUIDUpdatePacket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;


public class UUIDCache {

    private static final Map<String, UUID> nameUuidMap = new ConcurrentHashMap<>();
    private static final Map<UUID, String> uuidNameMap = new ConcurrentHashMap<>();

    private static UUIDCache instance;
    private final RedisService redisService;

    public UUIDCache(RedisService redisService) {
        if (instance != null)
            throw new IllegalStateException("Already Initialized");

        this.redisService = redisService;
        instance = this;
        //Bukkit.getPluginManager().registerEvents(this, plugin);
        this.redisService.executeCommand(redis -> {
            for (Map.Entry<String, String> entry : redis.hgetAll("UUIDCache").entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                String name = entry.getValue();

                nameUuidMap.put(name.toLowerCase(), uuid);
                uuidNameMap.put(uuid, name);
            }
            return null;
        });
    }

    public static UUID getUuid(String name) {
        return name == null ? null : nameUuidMap.get(name.toLowerCase());
    }

    public static UUID uuid(String name) {
        return getUuid(name);
    }

    public static String getName(UUID uuid) {
        return uuid == null ? null : uuidNameMap.get(uuid);
    }

    public static String name(UUID uuid) {
        return getName(uuid);
    }

    public void update(UUID uuid, String name, boolean async) {
        String oldName = uuidNameMap.get(uuid);
        if (name.equals(oldName)) return; // nothing changed — skip the Redis write and broadcast

        updateLocally(uuid, oldName, name);

        Runnable runnable;
        runnable = () ->
                this.redisService.executeCommand(redis -> {
                    redis.hset("UUIDCache", uuid.toString(), name);
                    return null;
                });

        if (async)
            Altara.TASK_CHAIN.run(runnable);
        else runnable.run();

        Altara.getRedisService().publish(new UUIDUpdatePacket(uuid, oldName, name));
    }

    public static void updateLocally(UUID uuid, String oldName, String newName) {
        if (uuid == null || newName == null) return;
        // Remove before put: a case-only rename maps to the same key.
        if (oldName != null) nameUuidMap.remove(oldName.toLowerCase(), uuid);
        nameUuidMap.put(newName.toLowerCase(), uuid);
        uuidNameMap.put(uuid, newName);
    }

    public void saveAll() {
        this.redisService.executeCommand(redis -> {
            uuidNameMap.forEach((uuid, s) -> redis.hset("UUIDCache", uuid.toString(), s));
            return null;
        });
    }

    public int getCachedAmount() {
        return uuidNameMap.size();
    }

    public Map<String, UUID> getNameUuidMap() {
        return nameUuidMap;
    }

    public Map<UUID, String> getUuidNameMap() {
        return uuidNameMap;
    }
}