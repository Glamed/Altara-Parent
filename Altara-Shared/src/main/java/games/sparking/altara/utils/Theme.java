package games.sparking.altara.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Altara's design tokens.  Every colour in player-facing output has a semantic
 * meaning — pick the token for what the text <em>is</em>, never a raw colour.
 *
 * <ul>
 *   <li>{@link #PRIMARY} aqua — feature identity, headings, highlighted nouns</li>
 *   <li>{@link #PRIMARY_DARK} dark aqua — structural accents (bars, arrows, CTAs)</li>
 *   <li>{@link #TEXT} gray — descriptive prose</li>
 *   <li>{@link #TEXT_STRONG} white — names and dynamic values inside prose</li>
 *   <li>{@link #STRUCTURE} dark gray — brackets, separators, frames</li>
 *   <li>green / red — success, enabled, online / error, disabled, offline</li>
 *   <li>{@link #WARNING} yellow — pending or in-review states</li>
 * </ul>
 */
public final class Theme {

    public static final TextColor PRIMARY      = NamedTextColor.AQUA;
    public static final TextColor PRIMARY_DARK = NamedTextColor.DARK_AQUA;
    public static final TextColor TEXT         = NamedTextColor.GRAY;
    public static final TextColor TEXT_STRONG  = NamedTextColor.WHITE;
    public static final TextColor STRUCTURE    = NamedTextColor.DARK_GRAY;
    public static final TextColor SUCCESS      = NamedTextColor.GREEN;
    public static final TextColor SUCCESS_DARK = NamedTextColor.DARK_GREEN;
    public static final TextColor ERROR        = NamedTextColor.RED;
    public static final TextColor ERROR_DARK   = NamedTextColor.DARK_RED;
    public static final TextColor WARNING      = NamedTextColor.YELLOW;
    public static final TextColor SPECIAL      = NamedTextColor.GOLD;

    /** GUI breadcrumb colours. */
    public static final TextColor TITLE_BRAND  = NamedTextColor.DARK_BLUE;
    public static final TextColor TITLE_PAGE   = NamedTextColor.BLACK;

    public static final String BRAND = "Altara";

    // ── Symbols ────────────────────────────────────────────────────────────────
    public static final String BAR      = "┃";
    public static final String ARROW    = "»";
    public static final String BACK     = "❰";
    public static final String CROSS    = "✕";
    public static final String CHECK    = "✔";
    public static final String DOT      = "●";
    public static final String RING     = "○";
    public static final String NAVIGATE = "➡";

    private Theme() {}
}
