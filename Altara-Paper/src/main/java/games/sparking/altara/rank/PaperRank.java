package games.sparking.altara.rank;

import com.google.gson.JsonObject;
import org.bukkit.Material;

import java.awt.*;
import java.util.Map;

/**
 * Paper-specific extension of {@link Rank} that adds Minecraft Material support.
 */
public class PaperRank extends Rank {

    public PaperRank() {
        super();
    }

    public PaperRank(String name) {
        super(name);
    }

    public PaperRank(JsonObject json) {
        super(json);
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

