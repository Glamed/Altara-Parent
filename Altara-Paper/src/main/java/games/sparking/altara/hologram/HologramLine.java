package games.sparking.altara.hologram;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Holds the raw (template) text for one hologram line.
 *
 * <p>Text may be stored in one of three formats and is automatically converted
 * to an Adventure {@link Component} by {@link #toComponent(String)}:
 * <ol>
 *   <li>Legacy <em>section-sign</em> ({@code §}) — produced by {@code CC.translate()}</li>
 *   <li>Legacy <em>ampersand</em> ({@code &}) — raw config strings</li>
 *   <li>MiniMessage — {@code <red>}, {@code <bold>}, etc.</li>
 * </ol>
 *
 * No entity references live here — entities are managed per-player inside {@link Hologram}.
 */
@Getter
public class HologramLine {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private String text;

    public HologramLine(String text) {
        this.text = text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * Converts a hologram line string to an Adventure {@link Component}.
     *
     * <ul>
     *   <li>If the string contains {@code §} it is treated as a legacy
     *       section-sign string (output of {@code ChatColor.translateAlternateColorCodes}).</li>
     *   <li>If the string contains {@code &} followed by a valid colour/format
     *       code it is treated as a legacy ampersand string.</li>
     *   <li>Otherwise the string is parsed as MiniMessage.</li>
     * </ul>
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (text.contains("\u00A7")) {                        // § — from CC.translate() / ChatColor
            return LegacyComponentSerializer.legacySection().deserialize(text);
        }
        if (containsLegacyAmpersand(text)) {                 // & colour codes
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
        return MINI_MESSAGE.deserialize(text);
    }

    /** Returns {@code true} if {@code text} contains a {@code &} followed by a known code char. */
    private static boolean containsLegacyAmpersand(String text) {
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '&') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ((code >= '0' && code <= '9')
                        || (code >= 'a' && code <= 'f')
                        || "klmnor".indexOf(code) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
