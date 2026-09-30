package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Altara's chat design system.  All player-facing chat output should be built from
 * these helpers so the visual language stays consistent (see {@link Theme}).
 *
 * <h2>Feedback</h2>
 * {@link #error}, {@link #notice} and {@link #success} produce the canonical single-line
 * messages: {@code ┃ Primary sentence. Secondary explanation.}  Wrap a dynamic value in
 * asterisks ({@code *PlayerName*}) to emphasise it — white inside the gray secondary
 * text, gray inside the coloured primary text.  Text passed to these helpers is
 * never parsed as MiniMessage, so player input is always safe.
 *
 * <h2>MiniMessage</h2>
 * {@link #format(String, TagResolver...)} parses trusted templates.  Never concatenate
 * player-controlled text into a template — pass it through a placeholder or
 * {@link #escape(String)} instead.
 */
public final class CC {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Visible width (in dashes) of a section header line. */
    private static final int LINE_WIDTH = 52;

    private CC() {}

    // ── Feedback messages ─────────────────────────────────────────────────────

    public static Component error(String primary) {
        return error(primary, null);
    }

    public static Component error(String primary, String secondary) {
        return status(Theme.ERROR_DARK, Theme.ERROR, primary, secondary);
    }

    public static Component error(Messages message, Object... args) {
        return error(message.getReason(), message.getMain().formatted(args));
    }

    public static Component notice(String primary) {
        return notice(primary, null);
    }

    public static Component notice(String primary, String secondary) {
        return status(Theme.PRIMARY_DARK, Theme.PRIMARY, primary, secondary);
    }

    /** A quiet notice with no primary heading: {@code ┃ <gray>message}. */
    public static Component info(String message) {
        return notice(null, message);
    }

    public static Component success(String primary) {
        return success(primary, null);
    }

    public static Component success(String primary, String secondary) {
        return status(Theme.SUCCESS_DARK, Theme.SUCCESS, primary, secondary);
    }

    /** Feedback line with pre-built components, for messages that need rich content. */
    public static Component error(ComponentLike primary, ComponentLike secondary) {
        return status(Theme.ERROR_DARK, Theme.ERROR, primary, secondary);
    }

    public static Component notice(ComponentLike primary, ComponentLike secondary) {
        return status(Theme.PRIMARY_DARK, Theme.PRIMARY, primary, secondary);
    }

    public static Component success(ComponentLike primary, ComponentLike secondary) {
        return status(Theme.SUCCESS_DARK, Theme.SUCCESS, primary, secondary);
    }

    // ── Destructive confirmation ──────────────────────────────────────────────

    /**
     * The three-line warning shown before a destructive command is confirmed:
     * <pre>
     * [!] You are about to &lt;action&gt;.
     * [!] &lt;consequence&gt;.
     * [!] Run the command again to confirm.
     * </pre>
     */
    public static List<Component> confirmation(String action, String consequence) {
        return List.of(
                warningMarker().append(Component.text("You are about to " + action + ".", Theme.ERROR)),
                warningMarker().append(Component.text(consequence, Theme.TEXT_STRONG)),
                warningMarker().append(Component.text("Run the command again to confirm.", Theme.TEXT))
        );
    }

    private static TextComponent warningMarker() {
        return Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text("!", Theme.ERROR_DARK, TextDecoration.BOLD))
                .append(Component.text("] ", Theme.STRUCTURE))
                .build();
    }

    // ── Panels (section headers) ──────────────────────────────────────────────
    //
    // A panel is a header line, body lines padded with one leading space, and a
    // closing line.  Its colours come from the {@link Panel} tier.

    /** {@code ------[Title]------} in the {@link Panel#PLAYER} colours. */
    public static Component header(String title) {
        return header(Panel.PLAYER, title, null);
    }

    /** {@code ------[Title > Subtitle]------} in the {@link Panel#PLAYER} colours. */
    public static Component header(String title, String subtitle) {
        return header(Panel.PLAYER, title, subtitle);
    }

    public static Component header(Panel panel, String title) {
        return header(panel, title, null);
    }

    /** {@code ------[Title > Subtitle]------}: bold title in the panel colour, gray {@code >}, white subtitle. */
    public static Component header(Panel panel, String title, String subtitle) {
        TextComponent.Builder label = Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text(title, panel.getTitle(), TextDecoration.BOLD));
        int length = LINE_WIDTH - title.length() - 2;

        if (subtitle != null && !subtitle.isEmpty()) {
            label.append(Component.text(" > ", Theme.TEXT))
                    .append(Component.text(subtitle, Theme.TEXT_STRONG));
            length -= subtitle.length() + 2;
        }
        label.append(Component.text("]", Theme.STRUCTURE));

        Component dashes = dashes(panel, Math.max(2, length / 2));
        return Component.text().append(dashes).append(label).append(dashes).build();
    }

    /** The closing line of a {@link Panel#PLAYER} panel. */
    public static Component footer() {
        return footer(Panel.PLAYER);
    }

    public static Component footer(Panel panel) {
        Component half = dashes(panel, LINE_WIDTH / 2);
        return Component.text().append(half).append(half).build();
    }

    private static Component dashes(Panel panel, int count) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < count; i++) {
            builder.append(Component.text("-", i % 2 == 0 ? Theme.STRUCTURE : panel.getLineAccent()));
        }
        return builder.build();
    }

    /** A panel body line: the content with one space of padding in front. */
    public static Component line(ComponentLike content) {
        return Component.text().append(Component.space()).append(content).build();
    }

    /** Gray panel body text, padded. */
    public static Component line(String text) {
        return line(Component.text(text, Theme.TEXT));
    }

    // ── Lists ─────────────────────────────────────────────────────────────────

    /** {@code  - Value} */
    public static Component item(String value) {
        return item(Component.text(value, Theme.TEXT_STRONG));
    }

    /** {@code  - Label: Value} */
    public static Component item(String label, String value) {
        return item(Component.text()
                .append(Component.text(label + ": ", Theme.TEXT))
                .append(Component.text(value, Theme.TEXT_STRONG))
                .build());
    }

    /** {@code  - Label: <component>} for values that carry their own colour. */
    public static Component item(String label, ComponentLike value) {
        return item(Component.text()
                .append(Component.text(label + ": ", Theme.TEXT))
                .append(value)
                .build());
    }

    public static Component item(ComponentLike content) {
        return Component.text()
                .append(Component.text(" - ", Theme.TEXT))
                .append(content)
                .build();
    }

    /** {@code   > Description} for nested information. */
    public static Component nested(String text) {
        return Component.text()
                .append(Component.text("  > ", Theme.PRIMARY_DARK))
                .append(Component.text(text, Theme.TEXT))
                .build();
    }

    /** Player-tier command help line: {@code  /command [args] - Description}. */
    public static Component command(String usage, String description) {
        return command(Panel.PLAYER, usage, description);
    }

    /** Command help line, {@code  /command [args] - Description}, in the panel's command colour. */
    public static Component command(Panel panel, String usage, String description) {
        TextComponent.Builder line = Component.text()
                .append(Component.space())
                .append(Component.text(usage, panel.getCommand()));
        if (description != null && !description.isEmpty()) {
            line.append(Component.text(" - ", Theme.TEXT))
                    .append(Component.text(description, Theme.TEXT_STRONG));
        }
        return line.clickEvent(ClickEvent.suggestCommand(usage.split(" [\\[(<]")[0] + " ")).build();
    }

    /** Understated empty state inside a panel: padded gray italics. */
    public static Component empty(String text) {
        return line(Component.text(text, Theme.TEXT, TextDecoration.ITALIC));
    }

    // ── States ────────────────────────────────────────────────────────────────

    /** {@code Enabled} / {@code Disabled} in green / red. */
    public static Component state(boolean enabled) {
        return state(enabled, "Enabled", "Disabled");
    }

    public static Component state(boolean positive, String positiveText, String negativeText) {
        return positive
                ? Component.text(positiveText, Theme.SUCCESS)
                : Component.text(negativeText, Theme.ERROR);
    }

    /** {@code (Enabled)} / {@code (Disabled)} with dark gray brackets — for setting names. */
    public static Component stateTag(boolean enabled) {
        return bracketed(state(enabled));
    }

    /** {@code (<component>)} with dark gray parentheses. */
    public static Component bracketed(ComponentLike content) {
        return Component.text()
                .append(Component.text("(", Theme.STRUCTURE))
                .append(content)
                .append(Component.text(")", Theme.STRUCTURE))
                .build();
    }

    /** Green / red filled dot for online / offline. */
    public static Component statusDot(boolean online) {
        return Component.text(Theme.DOT, online ? Theme.SUCCESS : Theme.ERROR);
    }

    /** Gray ring + italic "(pending)". */
    public static Component pending() {
        return Component.text()
                .append(Component.text(Theme.RING + " ", Theme.TEXT))
                .append(Component.text("(pending)", Theme.TEXT, TextDecoration.ITALIC))
                .build();
    }

    // ── Interactive ───────────────────────────────────────────────────────────

    /** {@code [➡]} clickable action with a gray hover explanation. */
    public static Component action(String hover, ClickEvent click) {
        return button(Theme.NAVIGATE, Theme.PRIMARY, hover, click);
    }

    /** {@code [✔]} clickable positive action. */
    public static Component accept(String hover, ClickEvent click) {
        return button(Theme.CHECK, Theme.SUCCESS, hover, click);
    }

    /** {@code [✕]} clickable negative action. */
    public static Component decline(String hover, ClickEvent click) {
        return button(Theme.CROSS, Theme.ERROR, hover, click);
    }

    private static Component button(String symbol, TextColor color, String hover, ClickEvent click) {
        return Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text(symbol, color, TextDecoration.BOLD))
                .append(Component.text("]", Theme.STRUCTURE))
                .hoverEvent(HoverEvent.showText(Component.text(hover, Theme.TEXT)))
                .clickEvent(click)
                .build();
    }

    // ── Progress bar ──────────────────────────────────────────────────────────

    public static Component bar(int total, int highlighted, TextColor primary, TextColor secondary) {
        TextComponent.Builder builder = Component.text().append(Component.text("[", Theme.STRUCTURE));
        for (int i = 0; i < total; i++) {
            builder.append(Component.text("■", i < highlighted ? primary : secondary));
        }
        return builder.append(Component.text("]", Theme.STRUCTURE)).build();
    }

    // ── MiniMessage ───────────────────────────────────────────────────────────

    /** Parses a trusted MiniMessage template. */
    public static Component format(String template, TagResolver... resolvers) {
        return MM.deserialize(template, resolvers);
    }

    public static List<Component> format(List<String> templates, TagResolver... resolvers) {
        List<Component> result = new ArrayList<>(templates.size());
        for (String line : templates) result.add(MM.deserialize(line, resolvers));
        return result;
    }

    /**
     * {@link String#format} into a MiniMessage template.  Arguments are inserted
     * <b>verbatim</b> and parsed as MiniMessage, so only pass trusted values (rank
     * prefixes, numbers, player names) — escape anything a player typed.
     */
    public static Component format(String template, Object... args) {
        return MM.deserialize(String.format(template, args));
    }

    /** Escapes MiniMessage tags so untrusted text renders literally. */
    public static String escape(String text) {
        return text == null ? "" : MM.escapeTags(text);
    }

    // ── Plain text ────────────────────────────────────────────────────────────

    public static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Removes MiniMessage tags from a template string. */
    public static String strip(String template) {
        return template == null ? "" : MM.stripTags(template);
    }

    public static String plural(int count, String singular) {
        return singular + (count == 1 ? "" : "s");
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static Component status(TextColor barColor, TextColor primaryColor, String primary, String secondary) {
        boolean hasPrimary   = primary != null && !primary.isEmpty();
        boolean hasSecondary = secondary != null && !secondary.isEmpty();

        return status(barColor, primaryColor,
                hasPrimary ? highlighted(primary, primaryColor, Theme.TEXT) : null,
                hasSecondary ? highlighted(secondary, Theme.TEXT, Theme.TEXT_STRONG) : null);
    }

    private static Component status(TextColor barColor, TextColor primaryColor,
                                    ComponentLike primary, ComponentLike secondary) {
        TextComponent.Builder builder = Component.text()
                .append(Component.text(Theme.BAR, barColor, TextDecoration.BOLD))
                .append(Component.space());

        if (primary != null) builder.append(Component.text().color(primaryColor).append(primary));
        if (primary != null && secondary != null) builder.append(Component.space());
        if (secondary != null) builder.append(Component.text().color(Theme.TEXT).append(secondary));
        return builder.build();
    }

    /** Splits on {@code *} and renders every odd segment in the highlight colour. */
    private static Component highlighted(String text, TextColor base, TextColor highlight) {
        TextComponent.Builder builder = Component.text();
        String[] parts = text.split("\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                builder.append(Component.text(parts[i], i % 2 == 1 ? highlight : base));
            }
        }
        return builder.build();
    }
}
