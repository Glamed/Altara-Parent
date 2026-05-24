package games.sparking.altara.hologram;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Supplies the text lines of a {@link Hologram} for a specific player.
 *
 * <p>Because the provider is called per-player, each player can see different content
 * (e.g. a personal stats board) while using the same {@link Hologram} instance.
 *
 * <p>Use the static factory helpers for simple, static text that is the same for everyone:
 * <pre>{@code
 * HologramProvider.fixed("&6§lMy Server", "&7Welcome back!");
 * }</pre>
 */
@FunctionalInterface
public interface HologramProvider {

    /**
     * Returns the lines that {@code player} should see. The first element is the
     * top-most line; subsequent elements go downward.
     */
    List<String> getLines(Player player);

    // =========================================================================
    // Static factory helpers
    // =========================================================================

    /** Creates a provider that shows the same fixed lines to every player. */
    static HologramProvider fixed(String... lines) {
        List<String> list = List.copyOf(Arrays.asList(lines));
        return player -> list;
    }

    /** Creates a provider that shows the same fixed lines to every player. */
    static HologramProvider fixed(List<String> lines) {
        List<String> list = List.copyOf(lines);
        return player -> list;
    }
}

