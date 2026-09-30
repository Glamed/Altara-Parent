package games.sparking.altara.menu;

import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Altara's inventory design system: breadcrumb titles, the standard frame, fixed
 * navigation buttons and lore grammar.
 *
 * <pre>
 * slot 0  — contextual help          slot 8  — {@link #closeButton()}
 * bottom-left (36 / 45) — {@link #backButton}
 * </pre>
 */
public final class Gui {

    public static final int CLOSE_SLOT = 8;
    public static final int HELP_SLOT  = 0;

    public static final Material BACKGROUND = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    public static final Material FRAME      = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    private Gui() {}

    // ── Titles ────────────────────────────────────────────────────────────────

    /** {@code Altara > Page > Subpage} breadcrumb. */
    public static Component title(String... pages) {
        TextComponent.Builder builder = Component.text()
                .append(Component.text(Theme.BRAND, Theme.TITLE_BRAND));
        for (String page : pages) {
            builder.append(Component.text(" > ", Theme.STRUCTURE))
                    .append(Component.text(page, Theme.TITLE_PAGE));
        }
        return builder.build();
    }

    /** Breadcrumb whose last page carries a semantic colour (e.g. dark red for Rules). */
    public static Component title(TextColor lastColor, String... pages) {
        TextComponent.Builder builder = Component.text()
                .append(Component.text(Theme.BRAND, Theme.TITLE_BRAND));
        for (int i = 0; i < pages.length; i++) {
            builder.append(Component.text(" > ", Theme.STRUCTURE))
                    .append(Component.text(pages[i], i == pages.length - 1 ? lastColor : Theme.TITLE_PAGE));
        }
        return builder.build();
    }

    // ── Lore ──────────────────────────────────────────────────────────────────

    /** {@code » Click to <action>} — the canonical call to action. */
    public static Component cta(String action) {
        return Component.text()
                .append(Component.text(Theme.ARROW + " ", Theme.PRIMARY_DARK, TextDecoration.BOLD))
                .append(Component.text("Click to " + action, Theme.TEXT_STRONG))
                .build();
    }

    /** Fluent lore builder that follows the description → values → blank → CTA grammar. */
    public static Lore lore() {
        return new Lore();
    }

    public static final class Lore {
        private final List<Component> lines = new ArrayList<>();

        /** Gray description line. Keep lines short — wrap manually. */
        public Lore text(String line) {
            lines.add(Component.text(line, Theme.TEXT));
            return this;
        }

        public Lore text(String... lines) {
            for (String line : lines) text(line);
            return this;
        }

        /** {@code - Label: Value}. */
        public Lore value(String label, String value) {
            return value(label, Component.text(value, Theme.TEXT_STRONG));
        }

        public Lore value(String label, ComponentLike value) {
            lines.add(Component.text()
                    .append(Component.text("- " + label + ": ", Theme.TEXT))
                    .append(value)
                    .build());
            return this;
        }

        /** {@code > Heading:} category marker. */
        public Lore heading(String heading) {
            lines.add(Component.text()
                    .append(Component.text("> ", Theme.PRIMARY_DARK))
                    .append(Component.text(heading + ":", Theme.TEXT_STRONG))
                    .build());
            return this;
        }

        /** {@code   - Value} entry under a {@link #heading}. */
        public Lore entry(String value) {
            lines.add(Component.text()
                    .append(Component.text("  - ", Theme.PRIMARY))
                    .append(Component.text(value, Theme.TEXT))
                    .build());
            return this;
        }

        public Lore line(ComponentLike line) {
            lines.add(line.asComponent());
            return this;
        }

        public Lore blank() {
            lines.add(Component.empty());
            return this;
        }

        /** Blank line followed by {@code » Click to <action>}. */
        public Lore cta(String action) {
            if (!lines.isEmpty()) blank();
            lines.add(Gui.cta(action));
            return this;
        }

        public List<Component> build() {
            return lines;
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    /** {@code Name (Enabled)} / {@code Name (Disabled)}. */
    public static Component settingName(String name, boolean enabled) {
        return Component.text()
                .append(Component.text(name + " ", Theme.TEXT_STRONG))
                .append(CC.stateTag(enabled))
                .build();
    }

    public static Material settingMaterial(boolean enabled) {
        return enabled ? Material.LIME_DYE : Material.GRAY_DYE;
    }

    // ── Standard items ────────────────────────────────────────────────────────

    public static ItemStack framePane() {
        return new ItemBuilder(FRAME).setDisplayName(Component.space()).build();
    }

    public static ItemStack backgroundPane() {
        return new ItemBuilder(BACKGROUND).setDisplayName(Component.space()).build();
    }

    /** Barrier in slot 8: {@code ✕ Close menu}. */
    public static Button closeButton() {
        return closeButton("Close menu");
    }

    public static Button closeButton(String label) {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.BARRIER)
                        .setDisplayName(Component.text()
                                .append(Component.text(Theme.CROSS + " ", Theme.TEXT))
                                .append(Component.text(label, Theme.ERROR))
                                .build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                player.closeInventory();
            }
        };
    }

    /** Arrow in the bottom-left: {@code ❰ <destination>}, opening the parent menu. */
    public static Button backButton(String destination, Supplier<Menu> parent) {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .setDisplayName(Component.text()
                                .append(Component.text(Theme.BACK + " ", Theme.TEXT))
                                .append(Component.text(destination, Theme.TEXT_STRONG))
                                .build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                parent.get().openMenu(player);
            }
        };
    }

    /** Bottom-left slot for a menu of the given size. */
    public static int backSlot(int size) {
        return size - 9;
    }

    /** {@code ✔ <label>} green/lime accept item for confirmation menus. */
    public static ItemStack acceptItem(String label, List<Component> lore) {
        return new ItemBuilder(Material.LIME_WOOL)
                .setDisplayName(Component.text()
                        .append(Component.text(Theme.CHECK + " ", Theme.SUCCESS_DARK))
                        .append(Component.text(label, Theme.SUCCESS))
                        .build())
                .setLore(lore)
                .build();
    }

    /** {@code ✕ <label>} red decline item for confirmation menus. */
    public static ItemStack declineItem(String label, List<Component> lore) {
        return new ItemBuilder(Material.RED_WOOL)
                .setDisplayName(Component.text()
                        .append(Component.text(Theme.CROSS + " ", Theme.ERROR_DARK))
                        .append(Component.text(label, Theme.ERROR))
                        .build())
                .setLore(lore)
                .build();
    }

    /** Item name in the primary colour, e.g. {@code General Commands}. */
    public static Component name(String name) {
        return Component.text(name, Theme.PRIMARY);
    }

    /** Two-tone item name, e.g. {@code Chat Channels} → aqua "Chat" + gray "Channels". */
    public static Component name(String primary, String secondary) {
        return Component.text()
                .append(Component.text(primary + " ", Theme.PRIMARY))
                .append(Component.text(secondary, Theme.TEXT))
                .build();
    }
}
