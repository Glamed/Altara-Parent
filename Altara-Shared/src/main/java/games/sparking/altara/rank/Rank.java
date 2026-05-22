package games.sparking.altara.rank;

import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.rank.packets.RankUpdatePacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.JsonObjClass;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;
import java.util.List;
import java.util.function.Consumer;

@Data
@NoArgsConstructor
public class Rank extends JsonObjClass {

    public static final Comparator<Rank> COMPARATOR =
            Collections.reverseOrder(Comparator.comparingInt(Rank::getWeight));

    private UUID uuid;
    private String name = "Unknown";

    private String prefix = "";
    private String suffix = "";
    private String symbol = "";
    private String description = "";

    private String color = "&7";
    private String chatColor = "&7";

    private int weight = 0;
    private int queuePriority = 0;

    private boolean primaryRank = true;
    private boolean defaultRank = false;
    private boolean disguisable = false;

    private String discordId;
    private List<String> permissions = new ArrayList<>();
    private List<String> localPermissions = new ArrayList<>();
    private List<UUID> inherits = new ArrayList<>();

    public Rank(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
    }

    public Rank(JsonObject json) {
        if (json.has("uuid") && !json.get("uuid").isJsonNull()) {
            this.uuid = UUID.fromString(json.get("uuid").getAsString());
        }

        if (json.has("name")) this.name = json.get("name").getAsString();
        if (json.has("prefix")) this.prefix = json.get("prefix").getAsString();
        if (json.has("suffix")) this.suffix = json.get("suffix").getAsString();
        if (json.has("symbol")) this.symbol = json.get("symbol").getAsString();
        if (json.has("description")) this.description = json.get("description").getAsString();
        if (json.has("color")) this.color = json.get("color").getAsString();
        if (json.has("chatColor")) this.chatColor = json.get("chatColor").getAsString();

        if (json.has("weight")) this.weight = json.get("weight").getAsInt();
        if (json.has("queuePriority")) this.queuePriority = json.get("queuePriority").getAsInt();

        if (json.has("primaryRank")) this.primaryRank = json.get("primaryRank").getAsBoolean();
        if (json.has("defaultRank")) this.defaultRank = json.get("defaultRank").getAsBoolean();
        if (json.has("disguisable")) this.disguisable = json.get("disguisable").getAsBoolean();

        if (json.has("discordId") && !json.get("discordId").isJsonNull()) {
            this.discordId = json.get("discordId").getAsString();
        }

        if (json.has("permissions")) {
            json.getAsJsonArray("permissions").forEach(element -> this.permissions.add(element.getAsString()));
        }

        if (json.has("localPermissions")) {
            json.getAsJsonArray("localPermissions").forEach(element -> this.localPermissions.add(element.getAsString()));
        }

        if (json.has("inherits")) {
            json.getAsJsonArray("inherits").forEach(element -> {
                try {
                    this.inherits.add(UUID.fromString(element.getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            });
        }
    }

    public void save(Consumer<String> feedback, Runnable callback) {
        this.save(feedback, true, callback);
    }

    public void save(Consumer<String> feedback, boolean update, Runnable callback) {
        Tasks.runAsync(() -> {
            JsonObject object = toJson();
            RequestResponse response = RequestHandler.put("api/rank", object);

            if (!response.wasSuccessful())
                feedback.accept("Could not save rank " + name + ": " + response.getErrorMessage() + " (" + response.getCode() + ")");
            else if (update)
                new RankUpdatePacket(uuid).publish();
            callback.run();
        });
    }


    @BsonIgnore
    public List<String> getInheritedPermissions() {
        List<String> inheritPermissions = new ArrayList<>();
        for (UUID inherit : inherits) {
            inheritPermissions.addAll(Altara.getSharedInstance().getRankService().getRank(inherit).getAllPermissions());
        }
        return inheritPermissions;
    }

    @BsonIgnore
    public List<String> getAllPermissions() {
        List<String> allPermissions = new ArrayList<>();
        allPermissions.addAll(getInheritedPermissions());
        allPermissions.addAll(permissions);
        return allPermissions;
    }

}