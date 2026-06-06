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
    public static final TextColor RED         = NamedTextColor.RED;
    public static final TextColor GRAY        = NamedTextColor.GRAY;

    public static final Component MENU_BAR       = Component.text("------------------------", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH);
    public static final Component CHAT_BAR       = Component.text("------------------------------------------------", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH);
    public static final Component SMALL_CHAT_BAR = Component.text("-----------------", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH);

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

    public static String plural(int count, String singular) {
        return singular + (count == 1 ? "" : "s");
    }

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
                ? Component.text(enabled, NamedTextColor.GREEN)
                : Component.text(disabled, NamedTextColor.RED);
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
        center.append(Component.text("[", NamedTextColor.DARK_GRAY));
        center.append(Component.text().color(headerColor).decorate(TextDecoration.BOLD).append(header).build());
        if (!subHeaderPlain.isEmpty()) {
            center.append(Component.text(" > ", NamedTextColor.DARK_GRAY));
            center.append(Component.text().color(subHeaderColor).append(subHeader).build());
        }
        center.append(Component.text("]", NamedTextColor.DARK_GRAY));

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
                .append(Component.text("❱ ", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text(reason + ":", NamedTextColor.AQUA))
                .build();
    }

    public static Component list(String reason, String main) {
        return Component.text()
                .append(Component.text("❱ ", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text(reason + ": ", NamedTextColor.AQUA))
                .append(Component.text("(" + main + ")", NamedTextColor.GRAY))
                .build();
    }

    public static Component errorMsg(Messages messages) {
        return errorMsg(messages.getReason(), messages.getMain());
    }

    public static Component errorMsg(Messages messages, Object... args) {
        return errorMsg(messages.getReason(), messages.getMain().formatted(args));
    }

    public static Component errorMsg(String reason) {
        return errorMsg(reason, null);
    }

    public static Component errorMsg(String reason, String main) {
        return buildStatusMsg("✕", NamedTextColor.DARK_RED, NamedTextColor.RED, NamedTextColor.GRAY, reason, main);
    }

    public static Component noticeMsg(Messages messages) {
        return noticeMsg(messages.getReason(), messages.getMain());
    }

    public static Component noticeMsg(String reason) {
        return noticeMsg(reason, null);
    }

    public static Component noticeMsg(String reason, String main) {
        return buildStatusMsg("┃", NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, NamedTextColor.GRAY, reason, main);
    }

    public static Component successMsg(Messages messages) {
        return successMsg(messages.getReason(), messages.getMain());
    }

    public static Component successMsg(String reason) {
        return successMsg(reason, null);
    }

    public static Component successMsg(String reason, String main) {
        return buildStatusMsg("✔", NamedTextColor.DARK_GREEN, NamedTextColor.GREEN, NamedTextColor.GRAY, reason, main);
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

        if (hasReason) builder.append(buildInlineFormatted(reason, reasonColor, NamedTextColor.WHITE));
        if (hasMain) {
            if (hasReason) builder.append(Component.text(" ", mainColor));
            builder.append(buildInlineFormatted(main, mainColor, NamedTextColor.WHITE));
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