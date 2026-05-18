package games.sparking.altara.uuid;


import games.sparking.altara.Altara;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.uuid.packets.UUIDUpdatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class UUIDCache {

    private static final Map<String, UUID> nameUuidMap = new HashMap<>();
    private static final Map<UUID, String> uuidNameMap = new HashMap<>();

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
        return nameUuidMap.get(name.toLowerCase());
    }

    public static UUID uuid(String name) {
        return nameUuidMap.get(name.toLowerCase());
    }

    public static String getName(UUID uuid) {
        return uuidNameMap.get(uuid);
    }

    public static String name(UUID uuid) {
        return uuidNameMap.get(uuid);
    }

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");
    public static boolean isUuid(String input) {
        return UUID_PATTERN.matcher(input).matches();
    }

    public void update(UUID uuid, String name, boolean async) {
        String oldName = uuidNameMap.getOrDefault(uuid, null);
        if (oldName != null)
            nameUuidMap.remove(oldName.toLowerCase());

        nameUuidMap.put(name.toLowerCase(), uuid);
        uuidNameMap.put(uuid, name);

        Runnable runnable;
        runnable = () ->
                this.redisService.executeCommand(redis -> {
                    redis.hset("UUIDCache", uuid.toString(), name);
                    return null;
                });

        if (async)
            Altara.TASK_CHAIN.run(runnable);
        else runnable.run();

        new UUIDUpdatePacket(uuid, oldName, name).publish();
    }

    public static void updateLocally(UUID uuid, String oldName, String newName) {
        nameUuidMap.put(newName.toLowerCase(), uuid);
        if (oldName != null)
            nameUuidMap.remove(oldName.toLowerCase());
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
