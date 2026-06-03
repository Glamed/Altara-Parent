package games.sparking.altara.framework.module.team;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * Colour identifiers for colour-based {@link GameTeam}s.
 *
 * <p>Each constant bundles together:
 * <ul>
 *   <li>A human-readable display name</li>
 *   <li>A BungeeCord {@link ChatColor} for chat/scoreboard formatting</li>
 *   <li>A Bukkit {@link DyeColor} for wool/terracotta placement</li>
 *   <li>A wool {@link Material} for GUI icons and map decorations</li>
 * </ul>
 */
@Getter
public enum TeamColor {

    RED    ("Red",    ChatColor.RED,          DyeColor.RED,    Material.RED_WOOL),
    BLUE   ("Blue",   ChatColor.BLUE,         DyeColor.BLUE,   Material.BLUE_WOOL),
    GREEN  ("Green",  ChatColor.GREEN,        DyeColor.LIME,   Material.LIME_WOOL),
    YELLOW ("Yellow", ChatColor.YELLOW,       DyeColor.YELLOW, Material.YELLOW_WOOL),
    AQUA   ("Aqua",   ChatColor.AQUA,         DyeColor.CYAN,   Material.CYAN_WOOL),
    WHITE  ("White",  ChatColor.WHITE,        DyeColor.WHITE,  Material.WHITE_WOOL),
    PINK   ("Pink",   ChatColor.LIGHT_PURPLE, DyeColor.PINK,   Material.PINK_WOOL),
    GRAY   ("Gray",   ChatColor.GRAY,         DyeColor.GRAY,   Material.GRAY_WOOL),
    ORANGE ("Orange", ChatColor.GOLD,         DyeColor.ORANGE, Material.ORANGE_WOOL),
    PURPLE ("Purple", ChatColor.DARK_PURPLE,  DyeColor.PURPLE, Material.PURPLE_WOOL);

    private final String displayName;
    private final ChatColor chatColor;
    private final DyeColor dyeColor;
    private final Material woolMaterial;

    TeamColor(String displayName, ChatColor chatColor, DyeColor dyeColor, Material woolMaterial) {
        this.displayName  = displayName;
        this.chatColor    = chatColor;
        this.dyeColor     = dyeColor;
        this.woolMaterial = woolMaterial;
    }

    /** Returns the display name wrapped in the team's chat colour, e.g. {@code "§cRed"}. */
    public String getColoredName() {
        return chatColor + displayName;
    }
}

