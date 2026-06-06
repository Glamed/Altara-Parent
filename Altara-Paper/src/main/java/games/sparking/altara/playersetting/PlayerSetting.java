package games.sparking.altara.playersetting;

import com.google.common.base.Splitter;
import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public abstract class PlayerSetting<T> {

    private final String parent;
    private final String key;
    private final Map<UUID, T> values = new HashMap<>();

    /**
     * When {@code true} this setting's value is persisted inside the player's
     * {@link games.sparking.altara.profile.ProfilePreferences} (and therefore
     * survives restarts / server switches) instead of being stored in Redis.
     *
     * <p>Set this field in an instance-initialiser block of the anonymous
     * subclass to opt-in, e.g.:
     * <pre>{@code
     * public static final PlayerSetting<String> MY_SETTING =
     *         new PlayerSetting<>("parent", "key") {
     *             { storedInProfile = true; }
     *             ...
     *         };
     * }</pre>
     */
    protected boolean storedInProfile = false;

    public static List<String> splitDescription(String description) {
        List<String> list = new ArrayList<>();
        List<String> split = new ArrayList<>(Splitter.fixedLength(25).splitToList(description));

        for (int i = 0; i < split.size(); i++) {
            String s = split.get(i);
            if (i < split.size() - 1 && !s.endsWith(" ") && !split.get(i + 1).startsWith(" ")) {
                s = s + "-";
            }
            list.add(CC.YELLOW + s.trim());
        }
        return list;
    }

    public abstract T getDefaultValue();

    public abstract T parse(String input);

    public T get(CommandSender sender) {
        if (!(sender instanceof Player))
            return getDefaultValue();

        return get((Player) sender);
    }

    public T get(Player player) {
        return values.getOrDefault(player.getUniqueId(), getDefaultValue());
    }

    public void set(Player player, T value) {
        values.put(player.getUniqueId(), value);

        if (storedInProfile) {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player.getUniqueId());
            if (profile != null) {
                profile.getOptions().getPreferences().set(parent + ":" + key, toString(value));
                profile.save(() -> {}, true);
            }
            return;
        }

        Tasks.runAsync(() -> Altara.getRedisService().executeCommand(redis ->
                redis.hset("playersettings:" + parent + ":" + key,
                        player.getUniqueId().toString(), toString(value))));
    }

    public void load(UUID uuid) {
        if (storedInProfile) {
            // Try the in-memory cache first; if the profile isn't loaded yet,
            // fetch it synchronously (safe — we are on an async pre-login thread).
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
            if (profile == null) {
                profile = Altara.getSharedInstance().getProfileService().loadProfile(uuid);
            }
            if (profile != null) {
                String raw = profile.getOptions().getPreferences().get(parent + ":" + key);
                values.put(uuid, raw != null ? parse(raw) : getDefaultValue());
            } else {
                values.put(uuid, getDefaultValue());
            }
            return;
        }

        values.put(uuid, Altara.getRedisService().executeCommand(redis -> {
            if (!redis.hexists("playersettings:" + parent + ":" + key, uuid.toString()))
                return getDefaultValue();

            return parse(redis.hget("playersettings:" + parent + ":" + key, uuid.toString()));
        }));
    }

    /** Whether this setting is persisted inside the player's Profile preferences. */
    public boolean isStoredInProfile() {
        return storedInProfile;
    }

    public void remove(UUID uuid) {
        values.remove(uuid);
    }

    public abstract ItemStack getIcon(Player player);

    public abstract void click(Player player, ClickType clickType);

    public boolean canUpdate(Player player) {
        return true;
    }

    public String toString(T value) {
        return value == null ? null : value.toString();
    }

}