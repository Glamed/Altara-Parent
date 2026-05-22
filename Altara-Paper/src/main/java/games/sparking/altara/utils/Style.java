package games.sparking.altara.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Style {

    BLUE(NamedTextColor.BLUE),
    AQUA(NamedTextColor.AQUA),
    YELLOW(NamedTextColor.YELLOW),
    RED(NamedTextColor.RED),
    GRAY(NamedTextColor.GRAY),
    GOLD(NamedTextColor.GOLD),
    GREEN(NamedTextColor.GREEN),
    WHITE(NamedTextColor.WHITE),
    BLACK(NamedTextColor.BLACK),

    DARK_BLUE(NamedTextColor.DARK_BLUE),
    DARK_AQUA(NamedTextColor.DARK_AQUA),
    DARK_GRAY(NamedTextColor.DARK_GRAY),
    DARK_GREEN(NamedTextColor.DARK_GREEN),
    DARK_PURPLE(NamedTextColor.DARK_PURPLE),
    DARK_RED(NamedTextColor.DARK_RED),

    LIGHT_PURPLE(NamedTextColor.LIGHT_PURPLE),

    STRIKETHROUGH(TextDecoration.STRIKETHROUGH),
    BOLD(TextDecoration.BOLD),
    ITALIC(TextDecoration.ITALIC),
    UNDERLINED(TextDecoration.UNDERLINED),
    OBFUSCATED(TextDecoration.OBFUSCATED);

    private final Object value;

    Style(TextColor color) {
        this.value = color;
    }

    Style(TextDecoration decoration) {
        this.value = decoration;
    }

    public boolean isColor() {
        return value instanceof TextColor;
    }

    public boolean isDecoration() {
        return value instanceof TextDecoration;
    }

    public TextColor getColor() {
        if (!isColor()) throw new IllegalStateException(name() + " is a decoration, not a color");
        return (TextColor) value;
    }

    public TextDecoration getDecoration() {
        if (!isDecoration()) throw new IllegalStateException(name() + " is a color, not a decoration");
        return (TextDecoration) value;
    }
}