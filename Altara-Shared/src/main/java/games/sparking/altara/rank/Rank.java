package games.sparking.altara.rank;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.rank.packets.RankUpdatePacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.awt.Color;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rank.  Colours / prefixes are MiniMessage strings.  Inherited ranks are stored by
 * UUID and resolved on demand, so an updated rank is picked up immediately by every
 * rank that inherits it.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@ToString(of = {"uuid", "name"})
public class Rank {

    public static final Comparator<Rank> COMPARATOR =
            Collections.reverseOrder(Comparator.comparingInt(Rank::getWeight));

    private static final Pattern LEGACY_INHERIT = Pattern.compile("Rank\\(uuid=([0-9a-fA-F-]{36})");

    private final UUID uuid;
    private String name;
    private String prefix = "<white>";
    private String suffix = "<white>";
    private String color = "<white>";
    private String chatColor = "<white>";

    private String description = "";
    private boolean team = false;

    private String discordId;
    private String staffDiscordId;

    private int weight = 0;
    private int queuePriority = 0;

    private boolean defaultRank = false;
    private boolean disguisable = false;

    private List<String> permissions = new ArrayList<>();
    /** Server-local permissions (permissions.json) — never sent to the API. */
    private List<String> localPermissions = new ArrayList<>();
    private List<UUID> inheritIds = new ArrayList<>();

    public Rank(JsonObject object) {
        this.uuid = UUID.fromString(object.get("uuid").getAsString());
        this.name = string(object, "name", "Unknown");
        this.prefix = string(object, "prefix", prefix);
        this.suffix = string(object, "suffix", suffix);
        this.color = string(object, "color", color);
        this.chatColor = string(object, "chatColor", chatColor);
        this.description = string(object, "description", "");
        this.weight = (int) number(object, "weight", 0);
        this.queuePriority = (int) number(object, "queuePriority", 0);
        this.team = bool(object, "team");
        this.defaultRank = bool(object, "defaultRank");
        this.disguisable = bool(object, "disguisable");
        this.discordId = string(object, "discordId", null);
        this.staffDiscordId = string(object, "staffDiscordId", null);
        this.permissions = stringList(object.get("permissions"));
        this.inheritIds = inheritList(object.get("inherits"));
        this.localPermissions = new ArrayList<>(Altara.getSharedInstance().getLocalPermissions(this));
    }

    public Rank(String name) {
        this.uuid = UUID.randomUUID();
        this.name = name;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("name", name);
        json.addProperty("prefix", prefix);
        json.addProperty("suffix", suffix);
        json.addProperty("color", color);
        json.addProperty("chatColor", chatColor);
        json.addProperty("description", description);
        json.addProperty("team", team);
        json.addProperty("weight", weight);
        json.addProperty("queuePriority", queuePriority);
        json.addProperty("defaultRank", defaultRank);
        json.addProperty("disguisable", disguisable);
        if (discordId != null) json.addProperty("discordId", discordId);
        if (staffDiscordId != null) json.addProperty("staffDiscordId", staffDiscordId);

        JsonArray permissionArray = new JsonArray();
        permissions.forEach(permissionArray::add);
        json.add("permissions", permissionArray);

        JsonArray inheritArray = new JsonArray();
        inheritIds.forEach(id -> inheritArray.add(id.toString()));
        json.add("inherits", inheritArray);
        return json;
    }

    public void save(Consumer<String> feedback, Runnable callback) {
        this.save(feedback, true, callback);
    }

    /** Saves and reports any failure to {@code sender} as an error message. */
    public void save(CommandSender sender, Runnable callback) {
        this.save(error -> sender.sendMessage(CC.error("Unable to save rank.", error)), true, callback);
    }

    public void save(Consumer<String> feedback, boolean update, Runnable callback) {
        Tasks.runAsync(() -> {
            RequestResponse response = RequestHandler.put("api/rank", toJson());

            if (!response.wasSuccessful())
                feedback.accept(response.getErrorMessage() + " (" + response.getCode() + ")");
            else if (update)
                new RankUpdatePacket(uuid).publish();
            callback.run();
        });
    }

    public String getDisplayName() {
        return this.color + this.name.replace('-', ' ');
    }

    // ── Inheritance ──────────────────────────────────────────────────────────

    /** Directly inherited ranks that still exist. */
    public List<Rank> getInherits() {
        List<Rank> inherits = new ArrayList<>();
        for (UUID id : inheritIds) {
            Rank rank = Altara.getSharedInstance().getRankService().getRank(id);
            if (rank != null) inherits.add(rank);
        }
        return inherits;
    }

    public boolean inherits(Rank rank) {
        return inheritIds.contains(rank.getUuid());
    }

    public void addInherit(Rank rank) {
        if (!inheritIds.contains(rank.getUuid())) inheritIds.add(rank.getUuid());
    }

    public void removeInherit(Rank rank) {
        inheritIds.remove(rank.getUuid());
    }

    /** Permissions from every inherited rank, recursively.  Safe against inheritance cycles. */
    public List<String> getInheritPermissions() {
        List<String> result = new ArrayList<>();
        collectInherited(this, new HashSet<>(Set.of(uuid)), result);
        return result;
    }

    private static void collectInherited(Rank rank, Set<UUID> visited, List<String> result) {
        for (Rank inherit : rank.getInherits()) {
            if (!visited.add(inherit.getUuid())) continue;
            collectInherited(inherit, visited, result);
            result.addAll(inherit.getPermissions());
            result.addAll(inherit.getLocalPermissions());
        }
    }

    /** Inherited permissions first, so this rank's own entries win when converted to a map. */
    public List<String> getAllPermissions() {
        List<String> allPermissions = new ArrayList<>(getInheritPermissions());
        allPermissions.addAll(this.permissions);
        allPermissions.addAll(this.localPermissions);
        return allPermissions;
    }

    // ── Icon ─────────────────────────────────────────────────────────────────

    /** Dye whose colour best matches this rank's colour, for GUI icons. */
    public Material getMaterial() {
        String stripped = getColor().toLowerCase().replaceAll("<[/!]?(bold|italic|underlined|strikethrough|obfuscated|[bionum])>", "");
        Matcher hex = Pattern.compile("#([0-9a-f]{6})").matcher(stripped);
        if (hex.find()) {
            return closestDye(Color.decode("#" + hex.group(1)));
        }

        String named = stripped.replaceAll("[<>]", "").replace("color:", "").trim();
        return switch (named) {
            case "green"                -> Material.LIME_DYE;
            case "aqua"                 -> Material.LIGHT_BLUE_DYE;
            case "red", "dark_red"      -> Material.RED_DYE;
            case "light_purple"         -> Material.MAGENTA_DYE;
            case "yellow"               -> Material.YELLOW_DYE;
            case "white"                -> Material.WHITE_DYE;
            case "blue", "dark_blue"    -> Material.BLUE_DYE;
            case "dark_green"           -> Material.GREEN_DYE;
            case "dark_aqua"            -> Material.CYAN_DYE;
            case "dark_purple"          -> Material.PURPLE_DYE;
            case "gold"                 -> Material.ORANGE_DYE;
            case "gray"                 -> Material.LIGHT_GRAY_DYE;
            case "dark_gray"            -> Material.GRAY_DYE;
            default                     -> Material.BLACK_DYE;
        };
    }

    private static Material closestDye(Color target) {
        Map<Material, Color> dyeColors = Map.ofEntries(
                Map.entry(Material.BLACK_DYE,      new Color(0, 0, 0)),
                Map.entry(Material.RED_DYE,        new Color(255, 0, 0)),
                Map.entry(Material.GREEN_DYE,      new Color(0, 128, 0)),
                Map.entry(Material.BLUE_DYE,       new Color(0, 0, 255)),
                Map.entry(Material.YELLOW_DYE,     new Color(255, 255, 0)),
                Map.entry(Material.CYAN_DYE,       new Color(0, 170, 170)),
                Map.entry(Material.MAGENTA_DYE,    new Color(255, 0, 255)),
                Map.entry(Material.ORANGE_DYE,     new Color(255, 165, 0)),
                Map.entry(Material.PURPLE_DYE,     new Color(128, 0, 128)),
                Map.entry(Material.LIGHT_BLUE_DYE, new Color(85, 255, 255)),
                Map.entry(Material.LIME_DYE,       new Color(85, 255, 85)),
                Map.entry(Material.GRAY_DYE,       new Color(85, 85, 85)),
                Map.entry(Material.LIGHT_GRAY_DYE, new Color(170, 170, 170)),
                Map.entry(Material.WHITE_DYE,      new Color(255, 255, 255)),
                Map.entry(Material.BROWN_DYE,      new Color(139, 69, 19)),
                Map.entry(Material.PINK_DYE,       new Color(255, 192, 203))
        );

        Material closest = Material.WHITE_DYE;
        double closestDistance = Double.MAX_VALUE;
        for (Map.Entry<Material, Color> entry : dyeColors.entrySet()) {
            Color c = entry.getValue();
            int r = target.getRed() - c.getRed(), g = target.getGreen() - c.getGreen(), b = target.getBlue() - c.getBlue();
            double distance = r * r + g * g + b * b;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entry.getKey();
            }
        }
        return closest;
    }

    // ── Lenient parsing (older saves stored every field as a string) ─────────

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static long number(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsLong();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    /** A JSON array, or a legacy {@code "[a, b]"} string produced by {@code toString()}. */
    private static List<String> stringList(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;

        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> result.add(e.getAsString()));
            return result;
        }

        String raw = element.getAsString().trim();
        if (raw.startsWith("[") && raw.endsWith("]")) raw = raw.substring(1, raw.length() - 1);
        for (String part : raw.split(",")) {
            if (!part.isBlank()) result.add(part.trim());
        }
        return result;
    }

    /**
     * A JSON array of UUIDs, or a legacy {@code toString()} dump of {@code Rank} objects —
     * only the top-level entries (not their own nested inherits) are taken.
     */
    private static List<UUID> inheritList(JsonElement element) {
        List<UUID> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;

        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> result.add(UUID.fromString(e.getAsString())));
            return result;
        }

        String raw = element.getAsString();
        int depth = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '[' || c == '(') depth++;
            else if (c == ']' || c == ')') depth--;
            else if (depth == 1 && raw.startsWith("Rank(uuid=", i)) {
                Matcher matcher = LEGACY_INHERIT.matcher(raw).region(i, raw.length());
                if (matcher.lookingAt()) result.add(UUID.fromString(matcher.group(1)));
            }
        }
        return result;
    }
}
