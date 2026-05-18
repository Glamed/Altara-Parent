package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class CC {

    // Color Constants
    public static final String BLUE = ChatColor.BLUE.toString();
    public static final String AQUA = ChatColor.AQUA.toString();
    public static final String YELLOW = ChatColor.YELLOW.toString();
    public static final String RED = ChatColor.RED.toString();
    public static final String GRAY = ChatColor.GRAY.toString();
    public static final String GOLD = ChatColor.GOLD.toString();
    public static final String GREEN = ChatColor.GREEN.toString();
    public static final String WHITE = ChatColor.WHITE.toString();
    public static final String BLACK = ChatColor.BLACK.toString();
    public static final String BOLD = ChatColor.BOLD.toString();
    public static final String ITALIC = ChatColor.ITALIC.toString();
    public static final String UNDER_LINE = ChatColor.UNDERLINE.toString();
    public static final String STRIKE_THROUGH = ChatColor.STRIKETHROUGH.toString();
    public static final String RESET = ChatColor.RESET.toString();
    public static final String MAGIC = ChatColor.MAGIC.toString();
    public static final String DBLUE = ChatColor.DARK_BLUE.toString();
    public static final String DAQUA = ChatColor.DARK_AQUA.toString();
    public static final String DGRAY = ChatColor.DARK_GRAY.toString();
    public static final String DGREEN = ChatColor.DARK_GREEN.toString();
    public static final String DPURPLE = ChatColor.DARK_PURPLE.toString();
    public static final String DRED = ChatColor.DARK_RED.toString();
    public static final String PURPLE = ChatColor.DARK_PURPLE.toString();
    public static final String PINK = ChatColor.LIGHT_PURPLE.toString();

    // Predefined Bars and Symbols
    public static final String MENU_BAR = GRAY + STRIKE_THROUGH + "------------------------";
    public static final String CHAT_BAR = GRAY + STRIKE_THROUGH + "------------------------------------------------";
    public static final String SMALL_CHAT_BAR = GRAY + STRIKE_THROUGH + "-----------------";
    public static final String SB_BAR = GRAY + STRIKE_THROUGH + "----------------------";
    public static final String VERTICAL_BAR = ChatColor.GRAY + "\u2503";
    public static final String HEART = DRED + "\u2764";
    public static final String LEFT_ARROW = ChatColor.GRAY + "\u00ab";
    public static final String RIGHT_ARROW = ChatColor.GRAY + "\u00bb";

    // Text Translators
    public static String translate(String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }

    public static List<String> translate(List<String> lines) {
        List<String> translated = new ArrayList<>();
        for (String line : lines) {
            translated.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return translated;
    }

    public static List<String> translate(String[] lines) {
        List<String> translated = new ArrayList<>();
        for (String line : lines) {
            if (line != null) {
                translated.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        return translated;
    }

    public static String strip(String in) {
        return ChatColor.stripColor(in);
    }

    // Formatting Helpers
    public static String format(String in, Object... args) {
        return String.format(translate(in), args);
    }

    public static List<String> format(List<String> lines, Object... args) {
        List<String> formatted = new ArrayList<>();
        for (String line : lines) {
            formatted.add(String.format(translate(line), args));
        }
        return formatted;
    }

    // Boolean Helpers
    public static String colorBoolean(boolean value) {
        return colorBoolean(value, false);
    }

    public static String colorBoolean(boolean value, boolean capitalize) {
        return colorBoolean(value,
                capitalize ? "Enabled" : "enabled",
                capitalize ? "Disabled" : "disabled");
    }

    public static String colorBoolean(boolean value, String enabled, String disabled) {
        return value ? GREEN + enabled : RED + disabled;
    }

    // Line Generation
    public static String genLine(String primaryColor, String secondaryColor) {
        return genLine(primaryColor, secondaryColor, null, "", null, "");
    }

    public static String genLine(String primaryColor, String secondaryColor, String headerColor, String header) {
        return genLine(primaryColor, secondaryColor, headerColor, header, null, "");
    }

    public static String genLine(String primaryColor, String secondaryColor, String headerColor, String header, String subHeaderColor, String subHeader) {
        int length = 52;

        if (!header.isEmpty()) {
            length -= header.length() + subHeader.length() + 2;
            if (!subHeader.isEmpty()) {
                subHeader = " &7> " + subHeaderColor + subHeader;
                length -= 2;
            }
            header = "&8[" + headerColor + "&l" + header + subHeader + "&8]";
        }

        StringBuilder main = new StringBuilder();
        boolean isPrimary = true;

        for (int i = 0; i < length / 2 - 1; i++) {
            main.append(isPrimary ? primaryColor : secondaryColor).append("-");
            isPrimary = !isPrimary;
        }

        return ChatColor.translateAlternateColorCodes('&', main + header + main);
    }

    public static String bar(float total, float highlighted, ChatColor primaryColor, ChatColor secondaryColor) {
        return bar(total, highlighted, primaryColor, secondaryColor, true);
    }

    public static String bar(float total, float highlighted, ChatColor primaryColor, ChatColor secondaryColor, boolean brackets) {
        StringBuilder primary = new StringBuilder();
        StringBuilder secondary = new StringBuilder();

        for (int i = 0; i < highlighted; i++) {
            primary.append(primaryColor).append("■");
        }
        for (int i = 0; i < total - highlighted; i++) {
            secondary.append(secondaryColor).append("■");
        }

        if (brackets) {
            return ChatColor.DARK_GRAY + "[" + primary + secondary + ChatColor.DARK_GRAY + "]";
        }
        return primary + secondary.toString();
    }

    public static ChatColor toType(String color) {
        color = color.replace("&", "");
        switch (color.toUpperCase()) {
            case "A":
                return ChatColor.GREEN;
            case "B":
                return ChatColor.AQUA;
            case "C":
                return ChatColor.RED;
            case "D":
                return ChatColor.LIGHT_PURPLE;
            case "E":
                return ChatColor.YELLOW;
            case "F":
                return ChatColor.WHITE;
            case "1":
                return ChatColor.DARK_BLUE;
            case "2":
                return ChatColor.DARK_GREEN;
            case "3":
                return ChatColor.DARK_AQUA;
            case "4":
                return ChatColor.DARK_RED;
            case "5":
                return ChatColor.DARK_PURPLE;
            case "6":
                return ChatColor.GOLD;
            case "7":
                return ChatColor.GRAY;
            case "8":
                return ChatColor.DARK_GRAY;
            case "9":
                return ChatColor.BLUE;
            case "0":
                return ChatColor.BLACK;
            default:
                return ChatColor.WHITE;
        }
    }

    // Messaging Utilities
    public static String list(String reason) {
        return ChatColor.translateAlternateColorCodes('&', "&3&l❱ &b" + reason + ":");
    }

    public static String list(String reason, String main) {
        return ChatColor.translateAlternateColorCodes('&', "&3&l❱ &b" + reason + ": &7(" + main + "&7)");
    }

    public static String errorMsg(Messages messages) {
        return errorMsg(messages.getReason(), messages.getMain());
    }

    public static String errorMsg(String messages) {
        return errorMsg(messages, null);
    }

    public static String errorMsg(String reason, String main) {
        if (main == "" || main == null) {
            return ChatColor.translateAlternateColorCodes('&', "&4\u2715 &c" + reason.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        } else if (reason == "" || reason == null) {
            return ChatColor.translateAlternateColorCodes('&', "&4\u2715 &7" + main.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        } else {
            return ChatColor.translateAlternateColorCodes('&', "&4\u2715 &c" + reason.replaceAll("\\*(.+?)\\*", "&f$1&7") + " &7" + main.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        }
    }

    public static String noticeMsg(String messages) {
        return noticeMsg(messages, null);
    }

    public static String noticeMsg(Messages messages) {
        return noticeMsg(messages.getReason(), messages.getMain());
    }

    public static String noticeMsg(String reason, String main) {
        if (main == "" || main == null) {
            return ChatColor.translateAlternateColorCodes('&', "&3&l\u2503 &b" + reason.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        } else if (reason == "" || reason == null) {
            return ChatColor.translateAlternateColorCodes('&', "&3&l\u2503 &7" + main.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        } else {
            return ChatColor.translateAlternateColorCodes('&', "&3&l\u2503 &b" + reason.replaceAll("\\*(.+?)\\*", "&f$1&7") + " &7" + main.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        }
    }

    public static String successMsg(Messages messages) {
        return successMsg(messages.getReason(), messages.getMain());
    }

    public static String successMsg(String reason, String main) {
        if (main == "" || main == null) {
            return ChatColor.translateAlternateColorCodes('&', "&2\u2714 &a" + reason.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        } else if (reason == "" || reason == null) {
            return ChatColor.translateAlternateColorCodes('&', "&2\u2714 &7" + main.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        } else {
            return ChatColor.translateAlternateColorCodes('&', "&2\u2714 &a" + reason.replaceAll("\\*(.+?)\\*", "&f$1&7") + " &7" + main.replaceAll("\\*(.+?)\\*", "&f$1&7"));
        }

    }

    // Adventure Component Helpers

    public static Component translateToComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static Component text(String text, TextColor color) {
        return Component.text(text, color);
    }

    public static Component text(String text, TextColor color, TextDecoration decoration) {
        return Component.text(text, color, decoration);
    }
}