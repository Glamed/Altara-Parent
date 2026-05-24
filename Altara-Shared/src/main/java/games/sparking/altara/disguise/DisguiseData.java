package games.sparking.altara.disguise;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Data
public class DisguiseData {

    private UUID uuid;

    private String disguiseName = "N/A";
    private Rank disguiseRank;

    private String skinName;
    private String texture;
    private String signature;

    private List<DisguiseLogEntry> logs = new ArrayList<>();

    public DisguiseData(UUID uuid) {
        this.uuid = uuid;
        this.disguiseRank = Altara.getSharedInstance().getRankService().getDefaultRank();
    }

    public DisguiseData(JsonObject object) {
        this.uuid = UUID.fromString(object.get("uuid").getAsString());
        this.disguiseName = object.get("disguiseName").getAsString();

        Rank rank = Altara.getSharedInstance().getRankService().getRank(UUID.fromString(object.get("disguiseRank").getAsString()));
        this.disguiseRank = rank == null ? Altara.getSharedInstance().getRankService().getDefaultRank() : rank;

        if (object.has("skinName"))
            this.skinName = object.get("skinName").getAsString();

        if (object.has("texture"))
            this.texture = object.get("texture").getAsString();

        if (object.has("signature"))
            this.signature = object.get("signature").getAsString();

        object.get("logs").getAsJsonArray().forEach(element -> logs.add(new DisguiseLogEntry(element.getAsJsonObject())));
    }

    public JsonObject toJson() {
        JsonBuilder builder = new JsonBuilder();
        builder.add("uuid", this.uuid);
        builder.add("disguiseName", this.disguiseName);
        builder.add("disguiseNameLowerCase", this.disguiseName.toLowerCase());

        if (this.disguiseRank == null) {
            builder.add("disguiseRank", Altara.getSharedInstance().getRankService().getDefaultRank().getUuid());
        } else {
            builder.add("disguiseRank", this.disguiseRank.getUuid().toString());
        }

        builder.add("skinName", this.skinName);
        builder.add("texture", this.texture);
        builder.add("signature", this.signature);

        JsonArray logArray = new JsonArray();
        logs.forEach(log -> logArray.add(log.toJson()));
        builder.add("logs", logArray);
        return builder.build();
    }

    public void save(Runnable callable, boolean async) {
        if (async) {
            Tasks.runAsync(() -> save(callable, false));
            return;
        }

        JsonObject body = toJson();
        RequestResponse response = RequestHandler.put("disguise", body);
        if (!response.wasSuccessful()) {
            if (response.getCode() == 404) {
                RequestResponse createResponse = RequestHandler.post("disguise", body);
                if (!createResponse.wasSuccessful())
                    Altara.getSharedInstance().getLogger().warn(String.format(
                            "Could not create disguise data of %s: %s (%d)",
                            uuid.toString(),
                            createResponse.getErrorMessage(),
                            createResponse.getCode()
                    ));

            } else Altara.getSharedInstance().getLogger().warn(String.format(
                    "Could not save disguise data of %s: %s (%d)",
                    uuid.toString(),
                    response.getErrorMessage(),
                    response.getCode()
            ));
        }

        callable.run();
    }

}
