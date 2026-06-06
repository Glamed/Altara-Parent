package games.sparking.altara.profile;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores arbitrary string key-value preferences for a {@link Profile}.
 *
 * <p>Preferences are persisted as part of {@link ProfileOptions} and therefore
 * survive server restarts.  They are intended for settings whose value should
 * follow the player across sessions (e.g. the active chat channel).
 *
 * <p>Keys are formatted as {@code "parent:key"} to mirror the PlayerSetting
 * namespace and avoid collisions between different providers.
 */
public class ProfilePreferences {

    private final Map<String, String> data = new HashMap<>();

    public ProfilePreferences() {}

    public ProfilePreferences(JsonObject object) {
        object.entrySet().forEach(entry ->
                data.put(entry.getKey(), entry.getValue().getAsString()));
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    public String get(String key) {
        return data.get(key);
    }

    public String get(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    // ── Write ──────────────────────────────────────────────────────────────────

    public void set(String key, String value) {
        data.put(key, value);
    }

    public void remove(String key) {
        data.remove(key);
    }

    // ── Serialisation ──────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        data.forEach(obj::addProperty);
        return obj;
    }
}


