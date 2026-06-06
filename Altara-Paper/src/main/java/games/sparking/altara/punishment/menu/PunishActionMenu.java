package games.sparking.altara.punishment.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.buttons.BackButton;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishmentManager;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Restriction-action builder menu — the second step of the punishment workflow.
 *
 * <p>Pre-loads the recommended {@link RestrictionAction}s for the selected
 * {@link InfractionType}. Staff can add, remove, or adjust durations before
 * clicking Confirm, which calls {@link PunishmentManager#issue()} to persist
 * and broadcast the punishment cross-server via the Web API + Redis.
 */
public class PunishActionMenu extends Menu {

    private static final int[] ACTION_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private Player        target;
    private InfractionType infractionType;
    private String        message;
    private Menu          backMenu;
    private final List<RestrictionAction> selectedActions = new ArrayList<>();

    public PunishActionMenu() {}

    public PunishActionMenu(Player target, InfractionType infractionType, String message, Menu backMenu) {
        initialize(target, infractionType, message, backMenu);
    }

    public PunishActionMenu initialize(Player target, InfractionType infractionType, String message, Menu backMenu) {
        this.target         = target;
        this.infractionType = infractionType;
        this.message        = message;
        this.backMenu       = backMenu;
        this.selectedActions.clear();
        this.selectedActions.addAll(infractionType.getRecommendedActions());
        return this;
    }

    // ── Action management ──────────────────────────────────────────────────────

    public void addRestriction(PunishmentType type, long duration) {
        selectedActions.add(new RestrictionAction(type, duration));
    }

    public void updateRestriction(int index, long duration) {
        RestrictionAction old = selectedActions.get(index);
        selectedActions.set(index, new RestrictionAction(old.getType(), duration));
    }

    public void removeRestriction(int index) {
        if (index >= 0 && index < selectedActions.size()) {
            selectedActions.remove(index);
        }
    }

    public List<RestrictionAction> getSelectedActions() {
        return new ArrayList<>(selectedActions);
    }

    // ── Menu overrides ─────────────────────────────────────────────────────────

    @Override
    public String getTitle(Player player) {
        return "Actions for " + target.getName();
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4,  new InfoButton());
        buttons.put(45, new BackButton(backMenu));
        buttons.put(47, new ClearButton());
        buttons.put(49, new ConfirmButton());
        buttons.put(53, new AddRestrictionButton());

        int max = Math.min(ACTION_SLOTS.length, selectedActions.size());
        for (int i = 0; i < max; i++) {
            buttons.put(ACTION_SLOTS[i], new SelectedActionButton(i, selectedActions.get(i)));
        }
        return buttons;
    }

    // ── Inner button classes ───────────────────────────────────────────────────

    private class InfoButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.BOOK)
                    .setDisplayName("<dark_purple><bold>Punishment Builder")
                    .setLore(
                            "<gray>Violation<dark_gray>: <light_purple>" + infractionType.getDisplayName(),
                            "<gray>Target<dark_gray>: <light_purple>" + target.getName(),
                            "<gray>Selected restrictions<dark_gray>: <light_purple>" + selectedActions.size(),
                            "",
                            "<light_purple>Recommended actions are preloaded.",
                            "<light_purple>Adjust durations or add more before confirming."
                    )
                    .build();
        }
    }

    private class SelectedActionButton extends Button {

        private final int              index;
        private final RestrictionAction action;

        private SelectedActionButton(int index, RestrictionAction action) {
            this.index  = index;
            this.action = action;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(getMaterial(action.getType()))
                    .setDisplayName("<dark_purple>" + action.getType().getDisplayName())
                    .setLore(
                            "<gray>Type<dark_gray>: <light_purple>"   + action.getType().getDisplayName(),
                            "<gray>Length<dark_gray>: <light_purple>" + formatDuration(action.getDuration()),
                            "",
                            "<yellow>Left click to change duration",
                            "<yellow>Right click to remove"
                    )
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (clickType == ClickType.RIGHT) {
                removeRestriction(index);
                openMenu(player);
                return;
            }
            new DurationSelectionMenu(PunishActionMenu.this, action.getType(), index, PunishActionMenu.this)
                    .openMenu(player);
        }
    }

    private class AddRestrictionButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.ANVIL)
                    .setDisplayName("<green>Add Restriction")
                    .setLore("<gray>Open a submenu to add a new", "<gray>restriction and pick its length.")
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            new RestrictionTypeMenu(PunishActionMenu.this).openMenu(player);
        }
    }

    private class ClearButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("<red>Clear Actions")
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            selectedActions.clear();
            openMenu(player);
        }
    }

    private class ConfirmButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Issue all selected restrictions");
            lore.add("<gray>as one violation action.");
            if (selectedActions.isEmpty()) {
                lore.add("");
                lore.add("<red>Add at least one restriction first.");
            }
            return new ItemBuilder(Material.LIME_CONCRETE)
                    .setDisplayName("<green>Confirm Punishment")
                    .setLore(CC.translate(lore))
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (selectedActions.isEmpty()) {
                player.sendMessage(CC.translate("<red>You need at least one restriction."));
                return;
            }
            new ConfirmationMenu(
                    "Issue " + selectedActions.size() + " action(s)?",
                    confirmed -> {
                        if (!confirmed) return;
                        new PunishmentManager(player, target, getSelectedActions(), infractionType, message).issue();
                        player.closeInventory();
                        player.sendMessage(CC.translate("<green>Punishment issued for <light_purple>" + target.getName() + "<green>. It will be applied cross-server."));
                    }
            ).openMenu(player);
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    public static Material getMaterial(PunishmentType type) {
        return switch (type) {
            case SUSPENSION         -> Material.IRON_BARS;
            case CHAT_RESTRICTION   -> Material.PAPER;
            case DISCORD_RESTRICTION -> Material.BOOK;
            case COMP_GAMEPLAY      -> Material.DIAMOND_SWORD;
            case REPORT             -> Material.WRITABLE_BOOK;
            case WARN               -> Material.YELLOW_DYE;
        };
    }

    private static String formatDuration(long duration) {
        if (duration == -1L) return "Permanent";
        if (duration <= 0L)  return "Immediate";
        return Time.formatDetailed(duration);
    }

    // ── Nested sub-menus ───────────────────────────────────────────────────────

    private static class RestrictionTypeMenu extends Menu {

        private final PunishActionMenu parent;

        private RestrictionTypeMenu(PunishActionMenu parent) {
            this.parent = parent;
        }

        @Override
        public String getTitle(Player player) {
            return "Add restriction";
        }

        @Override
        public int getSize() {
            return 27;
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            buttons.put(10, new TypeChoiceButton(PunishmentType.WARN));
            buttons.put(11, new TypeChoiceButton(PunishmentType.CHAT_RESTRICTION));
            buttons.put(12, new TypeChoiceButton(PunishmentType.DISCORD_RESTRICTION));
            buttons.put(13, new TypeChoiceButton(PunishmentType.REPORT));
            buttons.put(14, new TypeChoiceButton(PunishmentType.COMP_GAMEPLAY));
            buttons.put(15, new TypeChoiceButton(PunishmentType.SUSPENSION));
            buttons.put(18, new BackButton(parent));
            return buttons;
        }

        private class TypeChoiceButton extends Button {
            private final PunishmentType type;

            private TypeChoiceButton(PunishmentType type) {
                this.type = type;
            }

            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(getMaterial(type))
                        .setDisplayName("<dark_purple>" + type.getDisplayName())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (type == PunishmentType.WARN) {
                    parent.addRestriction(type, 0);
                    parent.openMenu(player);
                    return;
                }
                new DurationSelectionMenu(parent, type, null, RestrictionTypeMenu.this).openMenu(player);
            }
        }
    }

    private static class DurationSelectionMenu extends Menu {

        private final PunishActionMenu parent;
        private final PunishmentType   type;
        private final Integer          editIndex;
        private final Menu             backMenu;

        private DurationSelectionMenu(PunishActionMenu parent, PunishmentType type,
                                      Integer editIndex, Menu backMenu) {
            this.parent    = parent;
            this.type      = type;
            this.editIndex = editIndex;
            this.backMenu  = backMenu;
        }

        @Override
        public String getTitle(Player player) {
            return "Length: " + type.getDisplayName();
        }

        @Override
        public int getSize() {
            return 45;
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            buttons.put(10, new DurationButton("15m",  15L   * 60 * 1000));
            buttons.put(11, new DurationButton("30m",  30L   * 60 * 1000));
            buttons.put(12, new DurationButton("1h",   60L   * 60 * 1000));
            buttons.put(13, new DurationButton("6h",   6L    * 60 * 60 * 1000));
            buttons.put(14, new DurationButton("12h",  12L   * 60 * 60 * 1000));
            buttons.put(15, new DurationButton("1d",   24L   * 60 * 60 * 1000));
            buttons.put(19, new DurationButton("3d",   3L    * 24 * 60 * 60 * 1000));
            buttons.put(20, new DurationButton("7d",   7L    * 24 * 60 * 60 * 1000));
            buttons.put(21, new DurationButton("14d",  14L   * 24 * 60 * 60 * 1000));
            buttons.put(22, new DurationButton("30d",  30L   * 24 * 60 * 60 * 1000));
            buttons.put(23, new DurationButton("Permanent", -1L));
            buttons.put(36, new BackButton(backMenu));
            return buttons;
        }

        private class DurationButton extends Button {
            private final String label;
            private final long   duration;

            private DurationButton(String label, long duration) {
                this.label    = label;
                this.duration = duration;
            }

            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.CLOCK)
                        .setDisplayName("<light_purple>" + label)
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (editIndex == null) {
                    parent.addRestriction(type, duration);
                } else {
                    parent.updateRestriction(editIndex, duration);
                }
                parent.openMenu(player);
            }
        }
    }
}

