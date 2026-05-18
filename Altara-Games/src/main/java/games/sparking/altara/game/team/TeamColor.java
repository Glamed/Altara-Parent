package games.sparking.altara.game.team;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;

/**
 * Pre-defined colour options for {@link GameTeam}s.
 * Each constant exposes a {@link ChatColor} for chat prefixes,
 * a Bukkit {@link Color} for armour colouring, and a {@link DyeColor}
 * for wool / banner colouring.
 */
@Getter
@RequiredArgsConstructor
public enum TeamColor {

    RED(ChatColor.RED, Color.RED, DyeColor.RED),
    BLUE(ChatColor.BLUE, Color.BLUE, DyeColor.BLUE),
    GREEN(ChatColor.GREEN, Color.LIME, DyeColor.LIME),
    YELLOW(ChatColor.YELLOW, Color.YELLOW, DyeColor.YELLOW),
    AQUA(ChatColor.AQUA, Color.AQUA, DyeColor.LIGHT_BLUE),
    PURPLE(ChatColor.DARK_PURPLE, Color.PURPLE, DyeColor.PURPLE),
    ORANGE(ChatColor.GOLD, Color.ORANGE, DyeColor.ORANGE),
    PINK(ChatColor.LIGHT_PURPLE, Color.FUCHSIA, DyeColor.PINK),
    WHITE(ChatColor.WHITE, Color.WHITE, DyeColor.WHITE),
    GRAY(ChatColor.GRAY, Color.SILVER, DyeColor.LIGHT_GRAY),
    DARK_GREEN(ChatColor.DARK_GREEN, Color.GREEN, DyeColor.GREEN),
    DARK_RED(ChatColor.DARK_RED, Color.MAROON, DyeColor.RED),
    DARK_AQUA(ChatColor.DARK_AQUA, Color.TEAL, DyeColor.CYAN),
    DARK_GRAY(ChatColor.DARK_GRAY, Color.GRAY, DyeColor.GRAY);

    private final ChatColor chatColor;
    private final Color armorColor;
    private final DyeColor dyeColor;

    /** Returns the colour's chat prefix string (e.g. {@code "§c"}). */
    public String prefix() {
        return chatColor.toString();
    }
}


