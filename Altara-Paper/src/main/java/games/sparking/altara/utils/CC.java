package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.ArrayList;
import java.util.List;

public class CC {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // -------------------------------------------------------------------------
    // Color Constants
    // -------------------------------------------------------------------------
    public static final TextColor BLUE        = NamedTextColor.BLUE;
    public static final TextColor AQUA        = NamedTextColor.AQUA;
    public static final TextColor YELLOW      = NamedTextColor.YELLOW;
    public static final TextColor RED         = NamedTextColor.RED;
    public static final TextColor GRAY        = NamedTextColor.GRAY;
    public static final TextColor GOLD        = NamedTextColor.GOLD;
    public static final TextColor GREEN       = NamedTextColor.GREEN;
    public static final TextColor WHITE       = NamedTextColor.WHITE;
    public static final TextColor BLACK       = NamedTextColor.BLACK;
    public static final TextColor DBLUE       = NamedTextColor.DARK_BLUE;
    public static final TextColor DAQUA       = NamedTextColor.DARK_AQUA;
    public static final TextColor DGRAY       = NamedTextColor.DARK_GRAY;
    public static final TextColor DGREEN      = NamedTextColor.DARK_GREEN;
    public static final TextColor DPURPLE     = NamedTextColor.DARK_PURPLE;
    public static final TextColor DRED        = NamedTextColor.DARK_RED;
    public static final TextColor PURPLE      = NamedTextColor.DARK_PURPLE;
    public static final TextColor PINK        = NamedTextColor.LIGHT_PURPLE;

    // -------------------------------------------------------------------------
    // Predefined Bars and Symbols
    // -------------------------------------------------------------------------
    public static final Component MENU_BAR       = Component.text("------------------------", GRAY, TextDecoration.STRIKETHROUGH);
    public static final Component CHAT_BAR       = Component.text("------------------------------------------------", GRAY, TextDecoration.STRIKETHROUGH);
    public static final Component SMALL_CHAT_BAR = Component.text("-----------------", GRAY, TextDecoration.STRIKETHROUGH);
    public static final Component SB_BAR         = Component.text("----------------------", GRAY, TextDecoration.STRIKETHROUGH);
    public static final Component VERTICAL_BAR   = Component.text("\u2503", GRAY);
    public static final Component HEART          = Component.text("\u2764", DRED);
    public static final Component LEFT_ARROW     = Component.text("\u00ab", GRAY);
    public static final Component RIGHT_ARROW    = Component.text("\u00bb", GRAY);

    // -------------------------------------------------------------------------
    // MiniMessage Translation
    // -------------------------------------------------------------------------

    public static Component translate(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    /** Alias for {@link #translate(String)} — used by legacy call sites. */
    public static Component translateToComponent(String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    public static List<Component> translate(List<String> lines) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) result.add(translate(line));
        return result;
    }

    public static List<Component> translate(String[] lines) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            if (line != null) result.add(translate(line));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Format (MiniMessage + TagResolver placeholders)
    // -------------------------------------------------------------------------

    // Adventure Resolvers

    public static Component format(String line, TagResolver... resolvers) {
        return MM.deserialize(line, resolvers);
    }

    public static List<Component> format(List<String> lines, TagResolver... resolvers) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) result.add(MM.deserialize(line, resolvers));
        return result;
    }

    // String Resolvers

    public static Component format(String line, Object... resolvers) {
        return MM.deserialize(String.format(line, resolvers));
    }

    public static List<Component> format(List<String> lines, Object... resolvers) {
        List<Component> result = new ArrayList<>();
        for (String line : lines) {
            result.add(MM.deserialize(String.format(line, resolvers)));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Strip
    // -------------------------------------------------------------------------

    public static String strip(Component component) {
        return MM.stripTags(MM.serialize(component));
    }

    // -------------------------------------------------------------------------
    // Boolean Helpers
    // -------------------------------------------------------------------------

    public static Component colorBoolean(boolean value) {
        return colorBoolean(value, false);
    }

    public static Component colorBoolean(boolean value, boolean capitalize) {
        return colorBoolean(value,
                capitalize ? "Enabled" : "enabled",
                capitalize ? "Disabled" : "disabled");
    }

    public static Component colorBoolean(boolean value, String enabled, String disabled) {
        return value
                ? Component.text(enabled, GREEN)
                : Component.text(disabled, RED);
    }

    // -------------------------------------------------------------------------
    // Line Generation
    // -------------------------------------------------------------------------

    public static Component genLine(TextColor primary, TextColor secondary) {
        return genLine(primary, secondary, null, Component.empty(), null, Component.empty());
    }

    public static Component genLine(TextColor primary, TextColor secondary, TextColor headerColor, Component header) {
        return genLine(primary, secondary, headerColor, header, null, Component.empty());
    }

    public static Component genLine(TextColor primary, TextColor secondary,
                                    TextColor headerColor, Component header,
                                    TextColor subHeaderColor, Component subHeader) {
        String headerPlain    = toPlainText(header);
        String subHeaderPlain = toPlainText(subHeader);

        int length = 52;
        if (!headerPlain.isEmpty()) {
            length -= headerPlain.length() + subHeaderPlain.length() + 2;
        }
        int dashes = Math.max(0, length / 2 - 1);

        TextComponent.Builder dashBuilder = Component.text();
        boolean isPrimary = true;
        for (int i = 0; i < dashes; i++) {
            dashBuilder.append(Component.text("-", isPrimary ? primary : secondary));
            isPrimary = !isPrimary;
        }
        Component dashSegment = dashBuilder.build();

        if (headerPlain.isEmpty()) {
            return Component.text().append(dashSegment).append(dashSegment).build();
        }

        TextComponent.Builder center = Component.text();
        center.append(Component.text("[", DGRAY));
        center.append(Component.text().color(headerColor).decorate(TextDecoration.BOLD).append(header).build());
        if (!subHeaderPlain.isEmpty()) {
            center.append(Component.text(" > ", GRAY));
            center.append(Component.text().color(subHeaderColor).append(subHeader).build());
        }
        center.append(Component.text("]", DGRAY));

        return Component.text()
                .append(dashSegment)
                .append(center)
                .append(dashSegment)
                .build();
    }

    // -------------------------------------------------------------------------
    // Bar
    // -------------------------------------------------------------------------

    public static Component bar(int total, int highlighted, TextColor primary, TextColor secondary) {
        return bar(total, highlighted, primary, secondary, true);
    }

    public static Component bar(int total, int highlighted, TextColor primary, TextColor secondary, boolean brackets) {
        TextComponent.Builder builder = Component.text();
        if (brackets) builder.append(Component.text("[", NamedTextColor.DARK_GRAY));
        for (int i = 0; i < highlighted; i++)         builder.append(Component.text("■", primary));
        for (int i = 0; i < total - highlighted; i++) builder.append(Component.text("■", secondary));
        if (brackets) builder.append(Component.text("]", NamedTextColor.DARK_GRAY));
        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Messaging Utilities
    // -------------------------------------------------------------------------

    public static Component list(String reason) {
        return Component.text()
                .append(Component.text("❱ ", DAQUA, TextDecoration.BOLD))
                .append(Component.text(reason + ":", AQUA))
                .build();
    }

    public static Component list(String reason, String main) {
        return Component.text()
                .append(Component.text("❱ ", DAQUA, TextDecoration.BOLD))
                .append(Component.text(reason + ": ", AQUA))
                .append(Component.text("(" + main + ")", GRAY))
                .build();
    }

    public static Component errorMsg(Messages messages) {
        return errorMsg(messages.getReason(), messages.getMain());
    }

    public static Component errorMsg(String reason) {
        return errorMsg(reason, null);
    }

    public static Component errorMsg(String reason, String main) {
        return buildStatusMsg("\u2715", DRED, RED, GRAY, reason, main);
    }

    public static Component noticeMsg(Messages messages) {
        return noticeMsg(messages.getReason(), messages.getMain());
    }

    public static Component noticeMsg(String reason) {
        return noticeMsg(reason, null);
    }

    public static Component noticeMsg(String reason, String main) {
        return buildStatusMsg("\u2503", DAQUA, AQUA, GRAY, reason, main);
    }

    public static Component successMsg(Messages messages) {
        return successMsg(messages.getReason(), messages.getMain());
    }

    public static Component successMsg(String reason) {
        return successMsg(reason, null);
    }

    public static Component successMsg(String reason, String main) {
        return buildStatusMsg("\u2714", DGREEN, GREEN, GRAY, reason, main);
    }

    // -------------------------------------------------------------------------
    // Component Helpers
    // -------------------------------------------------------------------------

    public static Component text(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static Component text(String text, TextColor color, TextDecoration decoration) {
        return Component.text(text, color, decoration);
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private static Component buildStatusMsg(String icon,
                                            TextColor iconColor,
                                            TextColor reasonColor,
                                            TextColor mainColor,
                                            String reason,
                                            String main) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(icon + " ", iconColor, TextDecoration.BOLD));

        boolean hasReason = reason != null && !reason.isEmpty();
        boolean hasMain   = main   != null && !main.isEmpty();

        if (hasReason) builder.append(buildInlineFormatted(reason, reasonColor, WHITE));
        if (hasMain) {
            if (hasReason) builder.append(Component.text(" ", mainColor));
            builder.append(buildInlineFormatted(main, mainColor, WHITE));
        }

        return builder.build();
    }

    private static Component buildInlineFormatted(String text, TextColor baseColor, TextColor highlightColor) {
        TextComponent.Builder builder = Component.text();
        String[] parts = text.split("\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            builder.append(Component.text(parts[i], i % 2 == 1 ? highlightColor : baseColor));
        }
        return builder.build();
    }

    private static String toPlainText(Component component) {
        if (component == null) return "";
        StringBuilder sb = new StringBuilder();
        if (component instanceof TextComponent tc) sb.append(tc.content());
        for (Component child : component.children()) sb.append(toPlainText(child));
        return sb.toString();
    }
}