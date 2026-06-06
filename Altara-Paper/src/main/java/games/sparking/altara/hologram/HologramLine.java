package games.sparking.altara.hologram;

import games.sparking.altara.utils.CC;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

/**
 * Holds the raw (template) text for one hologram line.
 *
 * <p>All formatting is handled via MiniMessage through {@code CC.format()}.
 * Legacy § and & color codes are no longer supported.
 *
 * No entity references live here — entities are managed per-player inside {@link Hologram}.
 */
@Setter
@Getter
@AllArgsConstructor
public class HologramLine {

    private final String text;

    /**
     * Converts a hologram line string to an Adventure {@link Component}
     * using MiniMessage via {@code CC.format()}.
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return CC.format(text);
    }
}