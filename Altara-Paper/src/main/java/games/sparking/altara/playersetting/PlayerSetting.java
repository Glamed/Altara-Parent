package games.sparking.altara.playersetting;

import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.task.Tasks;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A per-player preference.  Values are loaded during pre-login and cached while the
 * player is online; reads are thread-safe (chat runs async).
 */
public abstract class PlayerSetting<T> {

    private final String parent;
    private final String key;
    private final Map<UUID, T> values = new ConcurrentHashMap<>();

    /**
     * When {@code true} the value is persisted in the player's
     * {@link games.sparking.altara.profile.ProfilePreferences} instead of Redis.  Opt in
     * from an instance initialiser: {@code { storedInProfile = true; }}.
     */
    protected boolean storedInProfile = false;

    protected PlayerSetting(String parent, String key) {
        this.parent = parent;
        this.key = key;
    }

    public abstract T getDefaultValue();

    public abstract T parse(String input);

    public T get(CommandSender sender) {
        return sender instanceof Player player ? get(player) : getDefaultValue();
    }

    public T get(Player player) {
        return values.getOrDefault(player.getUniqueId(), getDefaultValue());
    }

    public void set(Player player, T value) {
        if (value == null) values.remove(player.getUniqueId());
        else values.put(player.getUniqueId(), value);

        if (storedInProfile) {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player.getUniqueId());
            if (profile != null) {
                profile.getOptions().getPreferences().set(redisKey(), toString(value));
                profile.save(() -> {}, true);
            }
            return;
        }

        Tasks.runAsync(() -> Altara.getRedisService().executeCommand(redis ->
                redis.hset("playersettings:" + redisKey(), player.getUniqueId().toString(), toString(value))));
    }

    /** Loads the stored value.  Blocking — called from the async pre-login thread. */
    public void load(UUID uuid) {
        String raw;
        if (storedInProfile) {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
            if (profile == null) profile = Altara.getSharedInstance().getProfileService().loadProfile(uuid);
            raw = profile == null ? null : profile.getOptions().getPreferences().get(redisKey());
        } else {
            raw = Altara.getRedisService().executeCommand(redis -> redis.hget("playersettings:" + redisKey(), uuid.toString()));
        }

        T value = raw == null ? null : parse(raw);
        if (value == null) values.remove(uuid);
        else values.put(uuid, value);
    }

    private String redisKey() {
        return parent + ":" + key;
    }

    public boolean isStoredInProfile() {
        return storedInProfile;
    }

    public void remove(UUID uuid) {
        values.remove(uuid);
    }

    public abstract ItemStack getIcon(Player player);

    public abstract void click(Player player, ClickType clickType);

    /** Whether the player can see and change this setting in the menu. */
    public boolean canUpdate(Player player) {
        return true;
    }

    public String toString(T value) {
        return value == null ? null : value.toString();
    }
}
