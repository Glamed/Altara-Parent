package games.sparking.altara.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.utils.JsonObjClass;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class ProfileOptions extends JsonObjClass {

    private Map<String, String> customOptions = new HashMap<>();

    private List<String> socialSpy = new ArrayList<>();
    private List<UUID> ignoring = new ArrayList<>();

    /** Persistent player preferences (e.g. active chat channel). */
    private ProfilePreferences preferences = new ProfilePreferences();

    public ProfileOptions(JsonObject object) {
        if (object.has("socialSpy")) {
            JsonArray array = object.get("socialSpy").getAsJsonArray();
            array.forEach(element -> socialSpy.add(element.getAsString()));
        }

        if (object.has("ignoring")) {
            JsonArray array = object.get("ignoring").getAsJsonArray();
            array.forEach(element -> ignoring.add(UUID.fromString(element.getAsString())));
        }

        if (object.has("customOptions")) {
            JsonObject optionsObject = object.get("customOptions").getAsJsonObject();
            optionsObject.entrySet().forEach(entry ->
                    customOptions.put(entry.getKey(), entry.getValue().getAsString()));
        }

        if (object.has("preferences")) {
            this.preferences = new ProfilePreferences(object.get("preferences").getAsJsonObject());
        }
    }

    // ── Custom option helpers ──────────────────────────────────────────────────

    public String getOption(String key) {
        return customOptions.get(key);
    }

    public String getOption(String key, String defaultValue) {
        return customOptions.getOrDefault(key, defaultValue);
    }

    public void setOption(String key, String value) {
        customOptions.put(key, value);
    }

    // ── Serialisation (override broken reflection-based default) ───────────────

    @Override
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        JsonArray socialSpyArray = new JsonArray();
        socialSpy.forEach(socialSpyArray::add);
        obj.add("socialSpy", socialSpyArray);

        JsonArray ignoringArray = new JsonArray();
        ignoring.forEach(uuid -> ignoringArray.add(uuid.toString()));
        obj.add("ignoring", ignoringArray);

        JsonObject customOptionsObj = new JsonObject();
        customOptions.forEach(customOptionsObj::addProperty);
        obj.add("customOptions", customOptionsObj);

        obj.add("preferences", preferences.toJson());

        return obj;
    }
}