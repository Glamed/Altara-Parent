package games.sparking.altara.rank;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.rank.packets.RankUpdatePacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.JsonObjClass;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bukkit.Material;

import org.bukkit.command.CommandSender;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

@Data
public class Rank extends JsonObjClass {

    public static final Comparator<Rank> COMPARATOR =
            Collections.reverseOrder(Comparator.comparingInt(Rank::getWeight));

    private final UUID uuid;
    private String name;
    private String prefix = "&f";
    private String suffix = "&f";
    private String color = "&f";
    private String chatColor = "&f";

    private String description = "";
    private boolean team = false;

    private String discordId;
    private String staffDiscordId;

    private int weight = 0;
    private int queuePriority = 0;

    private boolean defaultRank = false;
    private boolean disguisable = false;

    private List<String> permissions = new ArrayList<>();
    private List<String> localPermissions = new ArrayList<>();
    private List<Rank> inherits = new ArrayList<>();

    public Rank(JsonObject object) {
        this.uuid = UUID.fromString(object.get("uuid").getAsString());
        this.name = object.get("name").getAsString();
        this.prefix = object.get("prefix").getAsString();
        this.suffix = object.get("suffix").getAsString();
        this.color = object.get("color").getAsString();
        this.chatColor = object.get("chatColor").getAsString();
        this.description = object.get("description").getAsString();
        this.weight = object.get("weight").getAsInt();
        this.team = object.get("team").getAsBoolean();
        this.queuePriority = object.get("queuePriority").getAsInt();
        this.defaultRank = object.get("defaultRank").getAsBoolean();
        this.disguisable = object.get("disguisable").getAsBoolean();
        object.get("permissions").getAsJsonArray().forEach(element -> permissions.add(element.getAsString()));

        if (object.has("discordId"))
            this.discordId = object.get("discordId").getAsString();

        if (object.has("staffDiscordId"))
            this.staffDiscordId = object.get("staffDiscordId").getAsString();

        this.localPermissions = Altara.getSharedInstance().getLocalPermissions(this);
    }

    public Rank(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
    }

    public void save(Consumer<String> feedback, Runnable callback) {
        this.save(feedback, true, callback);
    }

    public void save(CommandSender sender, Runnable callback) {
        this.save(sender::sendMessage, true, callback);
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
    };


    public String getDisplayName() {
        return this.color + this.name.replace('-', ' ');
    }

    public List<String> getInheritPermissions() {
        List<String> inheritPermissions = new ArrayList<>();
        for (Rank inherit : inherits) {
            inheritPermissions.addAll(inherit.getAllPermissions());
        }
        return inheritPermissions;
    }

    public List<String> getAllPermissions() {
        List<String> allPermissions = new ArrayList<>();
        allPermissions.addAll(this.getInheritPermissions());
        allPermissions.addAll(this.permissions);
        allPermissions.addAll(this.localPermissions);
        return allPermissions;
    }

    public Material getMaterial() {
        String stripped = getColor()
                .replaceAll("§[lomnk]", "")
                .replaceAll("§", "");

        String hex = null;

        // Handle §x§R§R§G§G§B§B format
        if (stripped.toLowerCase().startsWith("x") && stripped.length() == 13) {
            StringBuilder hexBuilder = new StringBuilder("#");
            for (int i = 1; i < stripped.length(); i += 2) {
                hexBuilder.append(stripped.charAt(i));
            }
            hex = hexBuilder.toString();
        }

        // Handle #RRGGBB format
        if (stripped.startsWith("#") && stripped.length() == 7) {
            hex = stripped;
        }

        if (hex != null) {
            try {
                Color color = Color.decode(hex);
                return closestDye(color);
            } catch (NumberFormatException ignored) {
            }
        }

        // Legacy MC color codes
        return switch (stripped.toLowerCase()) {
            case "a"      -> Material.LIME_DYE;
            case "b"      -> Material.LIGHT_BLUE_DYE;
            case "c", "4" -> Material.RED_DYE;
            case "d"      -> Material.MAGENTA_DYE;
            case "e"      -> Material.YELLOW_DYE;
            case "f"      -> Material.WHITE_DYE;
            case "1", "9" -> Material.BLUE_DYE;
            case "2"      -> Material.GREEN_DYE;
            case "3"      -> Material.CYAN_DYE;
            case "5"      -> Material.PURPLE_DYE;
            case "6"      -> Material.ORANGE_DYE;
            case "7"      -> Material.LIGHT_GRAY_DYE;
            case "8"      -> Material.GRAY_DYE;
            default       -> Material.BLACK_DYE;
        };
    }

    private Material closestDye(Color target) {
        Map<Material, Color> dyeColors = Map.ofEntries(
                Map.entry(Material.BLACK_DYE,      new Color(0, 0, 0)),
                Map.entry(Material.RED_DYE,        new Color(255, 0, 0)),
                Map.entry(Material.GREEN_DYE,      new Color(0, 255, 0)),
                Map.entry(Material.BLUE_DYE,       new Color(0, 0, 255)),
                Map.entry(Material.YELLOW_DYE,     new Color(255, 255, 0)),
                Map.entry(Material.CYAN_DYE,       new Color(0, 255, 255)),
                Map.entry(Material.MAGENTA_DYE,    new Color(255, 0, 255)),
                Map.entry(Material.ORANGE_DYE,     new Color(255, 165, 0)),
                Map.entry(Material.PURPLE_DYE,     new Color(128, 0, 128)),
                Map.entry(Material.LIGHT_BLUE_DYE, new Color(173, 216, 230)),
                Map.entry(Material.LIME_DYE,       new Color(50, 205, 50)),
                Map.entry(Material.GRAY_DYE,       new Color(128, 128, 128)),
                Map.entry(Material.LIGHT_GRAY_DYE, new Color(211, 211, 211)),
                Map.entry(Material.WHITE_DYE,      new Color(255, 255, 255)),
                Map.entry(Material.BROWN_DYE,      new Color(139, 69, 19)),
                Map.entry(Material.PINK_DYE,       new Color(255, 192, 203))
        );

        Material closest = Material.WHITE_DYE;
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<Material, Color> entry : dyeColors.entrySet()) {
            double distance = colorDistance(target, entry.getValue());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entry.getKey();
            }
        }

        return closest;
    }

    private double colorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed()   - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue()  - c2.getBlue();
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

}