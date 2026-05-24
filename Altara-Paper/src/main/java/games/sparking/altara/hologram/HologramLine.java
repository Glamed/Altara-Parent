package games.sparking.altara.hologram;

import lombok.Value;

/**
 * Immutable container for a single line of hologram text.
 *
 * <p>In the new hologram system the actual rendering is handled by EntityLib
 * {@code WrapperEntity} armor stands managed inside {@link Hologram}.  This class is
 * kept as a lightweight value type for situations where you want to pre-build a list of
 * lines before constructing a hologram (e.g. loading from config).
 */
@Value
public class HologramLine {

    String text;

    /** Creates a line with the given legacy-formatted text (supports {@code &} color codes). */
    public HologramLine(String text) {
        this.text = text;
    }
}