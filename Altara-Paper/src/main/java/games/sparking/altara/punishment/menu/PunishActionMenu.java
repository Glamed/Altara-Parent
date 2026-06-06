package games.sparking.altara.punishment.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.buttons.BackButton;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishManager;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PunishActionMenu extends Menu {

    private static final Material MENU_PANE = Material.GRAY_STAINED_GLASS_PANE;

    private static final int[] ACTION_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private final List<RestrictionAction> selectedActions = new ArrayList<>();
    private Player target;
    private InfractionType infractionType;
    private String message;
    private Menu backMenu;

    public PunishActionMenu() {
    }

    public PunishActionMenu(Player target, InfractionType infractionType, String message, Menu backMenu) {
        initialize(target, infractionType, message, backMenu);
    }

    public PunishActionMenu initialize(Player target, InfractionType infractionType, String message, Menu backMenu) {
        this.target = target;
        this.infractionType = infractionType;
        this.message = message;
        this.backMenu = backMenu;
        this.selectedActions.clear();
        for (RestrictionAction action : infractionType.getRecommendedActions()) {
            if (!hasActionType(action.getType())) {
                this.selectedActions.add(action);
            }
        }
        return this;
    }

    public boolean addRestriction(PunishmentType type, long duration) {
        if (hasActionType(type)) {
            return false;
        }
        selectedActions.add(new RestrictionAction(type, duration));
        return true;
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

    private boolean hasActionType(PunishmentType type) {
        for (RestrictionAction action : selectedActions) {
            if (action.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDuplicateActionTypes() {
        EnumSet<PunishmentType> seen = EnumSet.noneOf(PunishmentType.class);
        for (RestrictionAction action : selectedActions) {
            if (!seen.add(action.getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getTitle(Player player) {
        return CC.format("<light_purple>Actions for <dark_purple>" + target.getName());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4, new InfoButton());
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

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.BORDER;
    }

    @Override
    public ItemStack getPlaceholderItem(Player player) {
        return new ItemBuilder(MENU_PANE).setDisplayName(" ").build();
    }

    private Material getMaterial(PunishmentType type) {
        return switch (type) {
            case SUSPENSION          -> Material.IRON_BARS;
            case CHAT_RESTRICTION    -> Material.PAPER;
            case DISCORD_RESTRICTION -> Material.BOOK;
            case COMP_GAMEPLAY       -> Material.DIAMOND_SWORD;
            case REPORT              -> Material.WRITABLE_BOOK;
            case WARN                -> Material.YELLOW_DYE;
        };
    }

    private String formatDuration(long duration) {
        if (duration == -1) return "Permanent";
        if (duration <= 0)  return "Immediate";
        return Time.formatDetailed(duration);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inner menus
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Sub-menu for choosing which restriction type to add.
     */
    private static class RestrictionTypeMenu extends Menu {

        private static final Material MENU_PANE = Material.GRAY_STAINED_GLASS_PANE;
        private final PunishActionMenu parent;

        private RestrictionTypeMenu(PunishActionMenu parent) {
            this.parent = parent;
        }

        @Override
        public Component getTitle(Player player) {
            return CC.format("<light_purple>Add Restriction");
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

        @Override public FillTemplate getFillTemplate() { return FillTemplate.BORDER; }

        @Override
        public ItemStack getPlaceholderItem(Player player) {
            return new ItemBuilder(MENU_PANE).setDisplayName(" ").build();
        }

        private class TypeChoiceButton extends Button {
            private final PunishmentType type;

            private TypeChoiceButton(PunishmentType type) {
                this.type = type;
            }

            @Override
            public ItemStack getItem(Player player) {
                boolean alreadySelected = parent.hasActionType(type);
                return new ItemBuilder(parent.getMaterial(type))
                        .setDisplayName(CC.format((alreadySelected ? "<gray>" : "<dark_purple>") + type.getName()))
                        .setLore(alreadySelected
                                ? CC.format("<gray>Already selected")
                                : CC.format("<gray>Click to set duration"))
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (parent.hasActionType(type)) {
                    player.sendMessage(CC.errorMsg("That restriction type is already selected."));
                    return;
                }
                if (type == PunishmentType.WARN) {
                    parent.addRestriction(type, 0);
                    parent.openMenu(player);
                    return;
                }
                new DurationSelectionMenu(parent, type, null, RestrictionTypeMenu.this, 0L).openMenu(player);
            }
        }
    }

    /**
     * Sub-menu for picking the duration of a restriction.
     * Supports per-row increment stepping (minutes / hours / days / months)
     * as well as quick-preset buttons and a permanent toggle.
     */
    private static class DurationSelectionMenu extends Menu {

        private static final Material MENU_PANE = Material.GRAY_STAINED_GLASS_PANE;

        private static final int TYPE_MINUTES = 0;
        private static final int TYPE_HOURS   = 1;
        private static final int TYPE_DAYS    = 2;
        private static final int TYPE_MONTHS  = 3;

        /** Step sizes available for each time-type row. */
        private static final long[][] TIME_STEPS = {
                { 60_000L, 5*60_000L, 10*60_000L, 15*60_000L, 30*60_000L, 45*60_000L },          // Minutes
                { 3_600_000L, 2*3_600_000L, 3*3_600_000L, 6*3_600_000L, 12*3_600_000L, 24*3_600_000L }, // Hours
                { 86_400_000L, 3*86_400_000L, 7*86_400_000L, 14*86_400_000L, 21*86_400_000L, 30*86_400_000L }, // Days
                { 30*86_400_000L, 2*30*86_400_000L, 3*30*86_400_000L, 6*30*86_400_000L, 12*30*86_400_000L }    // Months
        };

        /** Quick-select preset durations shown in the bottom row. */
        private static final long[] PRESET_TIMES = {
                15 * 60_000L,               // 15 min
                3_600_000L,                 // 1 h
                6 * 3_600_000L,             // 6 h
                86_400_000L,                // 1 d
                7 * 86_400_000L,            // 7 d
                30 * 86_400_000L            // 30 d
        };

        private final PunishActionMenu parent;
        private final PunishmentType   type;
        private final Integer          editIndex;  // null = new restriction
        private final Menu             backMenu;

        private long  currentDuration    = 0L;
        private boolean isPermanent      = false;
        private int[] selectedStepIndexes = {0, 0, 0, 0};

        private DurationSelectionMenu(PunishActionMenu parent, PunishmentType type,
                                      Integer editIndex, Menu backMenu, long initialDuration) {
            this.parent    = parent;
            this.type      = type;
            this.editIndex = editIndex;
            this.backMenu  = backMenu;
            if (initialDuration == -1L) {
                this.isPermanent = true;
            } else {
                this.currentDuration = Math.max(0L, initialDuration);
            }
        }

        @Override
        public Component getTitle(Player player) {
            return CC.format("<light_purple>Duration: <dark_purple>" + type.getName());
        }

        @Override public int getSize() { return 54; }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();

            buttons.put(4, new TitleButton());
            buttons.put(8, new PermanentToggleButton());

            // Rows: minutes (11-15), hours (20-24), days (29-33), months (38-42)
            int[] rowStarts = {11, 20, 29, 38};
            for (int row = 0; row < 4; row++) {
                int base = rowStarts[row];
                buttons.put(base,     new IncrementDecrementButton(row));
                buttons.put(base + 1, new TimeAdjustButton(row, false));
                buttons.put(base + 3, new TimeAdjustButton(row, true));
                buttons.put(base + 4, new IncrementIncrementButton(row));
            }

            // Bottom row: presets + confirm + clear
            buttons.put(46, new PresetTimeButton(0));
            buttons.put(47, new PresetTimeButton(1));
            buttons.put(48, new PresetTimeButton(2));
            buttons.put(49, new ConfirmButton());
            buttons.put(50, new PresetTimeButton(3));
            buttons.put(51, new PresetTimeButton(4));
            buttons.put(52, new PresetTimeButton(5));
            buttons.put(53, new ClearButton());

            return buttons;
        }

        @Override public FillTemplate getFillTemplate() { return FillTemplate.BORDER; }

        @Override
        public ItemStack getPlaceholderItem(Player player) {
            return new ItemBuilder(MENU_PANE).setDisplayName(" ").build();
        }

        // ── Helpers ──────────────────────────────────────────────────────────

        private String getDurationLabel() {
            if (isPermanent) return "Permanent";
            return currentDuration == 0 ? "0s" : Time.formatDetailed(currentDuration);
        }

        private long getTimeIncrement(int typeIndex) {
            return TIME_STEPS[typeIndex][selectedStepIndexes[typeIndex]];
        }

        private String getTypeLabel(int typeIndex) {
            return switch (typeIndex) {
                case TYPE_MINUTES -> "Minutes";
                case TYPE_HOURS   -> "Hours";
                case TYPE_DAYS    -> "Days";
                case TYPE_MONTHS  -> "Months";
                default           -> "Unknown";
            };
        }

        // ── Buttons ───────────────────────────────────────────────────────────

        private class TitleButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.SKELETON_SKULL)
                        .setDisplayName(CC.format("<dark_purple><bold>Duration Selector"))
                        .setLore(CC.format("<gray>Current: <light_purple>" + getDurationLabel()))
                        .build();
            }

            @Override public void click(Player player, int slot, ClickType clickType, int hotbarButton) {}
        }

        private class PermanentToggleButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(isPermanent ? Material.PURPLE_DYE : Material.GRAY_DYE)
                        .setDisplayName(CC.format("<light_purple>Permanent"))
                        .setLore(
                                CC.format(isPermanent ? "<green>ENABLED" : "<gray>DISABLED"),
                                CC.format("<gray>Click to toggle")
                        )
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                isPermanent = !isPermanent;
                openMenu(player);
            }
        }

        private class IncrementDecrementButton extends Button {
            private final int typeIndex;
            IncrementDecrementButton(int typeIndex) { this.typeIndex = typeIndex; }

            @Override
            public ItemStack getItem(Player player) {
                long cur = getTimeIncrement(typeIndex);
                List<Component> lore = new ArrayList<>();
                lore.add(CC.format("<gray>Increment: <light_purple>" + Time.formatDetailed(cur)));
                if (selectedStepIndexes[typeIndex] > 0) {
                    long next = TIME_STEPS[typeIndex][selectedStepIndexes[typeIndex] - 1];
                    lore.add(CC.format("<gray>→ <light_purple>" + Time.formatDetailed(next)));
                } else {
                    lore.add(CC.format("<gray>(Already at minimum)"));
                }
                return new ItemBuilder(Material.ARROW)
                        .setDisplayName(CC.format("<red>< " + getTypeLabel(typeIndex)))
                        .setLore(lore)
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (selectedStepIndexes[typeIndex] > 0) {
                    selectedStepIndexes[typeIndex]--;
                    openMenu(player);
                }
            }
        }

        private class IncrementIncrementButton extends Button {
            private final int typeIndex;
            IncrementIncrementButton(int typeIndex) { this.typeIndex = typeIndex; }

            @Override
            public ItemStack getItem(Player player) {
                long cur = getTimeIncrement(typeIndex);
                List<Component> lore = new ArrayList<>();
                lore.add(CC.format("<gray>Increment: <light_purple>" + Time.formatDetailed(cur)));
                if (selectedStepIndexes[typeIndex] < TIME_STEPS[typeIndex].length - 1) {
                    long next = TIME_STEPS[typeIndex][selectedStepIndexes[typeIndex] + 1];
                    lore.add(CC.format("<gray>→ <light_purple>" + Time.formatDetailed(next)));
                } else {
                    lore.add(CC.format("<gray>(Already at maximum)"));
                }
                return new ItemBuilder(Material.ARROW)
                        .setDisplayName(CC.format("<green>" + getTypeLabel(typeIndex) + " >"))
                        .setLore(lore)
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (selectedStepIndexes[typeIndex] < TIME_STEPS[typeIndex].length - 1) {
                    selectedStepIndexes[typeIndex]++;
                    openMenu(player);
                }
            }
        }

        private class TimeAdjustButton extends Button {
            private final int typeIndex;
            private final boolean increase;

            TimeAdjustButton(int typeIndex, boolean increase) {
                this.typeIndex = typeIndex;
                this.increase  = increase;
            }

            @Override
            public ItemStack getItem(Player player) {
                long increment = getTimeIncrement(typeIndex);
                return new ItemBuilder(increase ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                        .setDisplayName(CC.format((increase ? "<green>+" : "<red>-") + " " + getTypeLabel(typeIndex)))
                        .setLore(
                                CC.format("<gray>Step: <light_purple>" + Time.formatDetailed(increment)),
                                CC.format("<gray>Total: <light_purple>" + getDurationLabel())
                        )
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                isPermanent = false;
                long increment = getTimeIncrement(typeIndex);
                currentDuration = Math.max(0, currentDuration + (increase ? increment : -increment));
                openMenu(player);
            }
        }

        private class PresetTimeButton extends Button {
            private final int index;
            PresetTimeButton(int index) { this.index = index; }

            @Override
            public ItemStack getItem(Player player) {
                long time = PRESET_TIMES[index];
                return new ItemBuilder(Material.CLOCK)
                        .setDisplayName(CC.format("<dark_purple>" + Time.formatDetailed(time)))
                        .setLore(CC.format("<gray>Click to set duration"))
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                currentDuration = PRESET_TIMES[index];
                isPermanent     = false;
                openMenu(player);
            }
        }

        private class ConfirmButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.GREEN_WOOL)
                        .setDisplayName(CC.format("<green>Confirm"))
                        .setLore(CC.format("<gray>Duration: <light_purple>" + getDurationLabel()))
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                long duration = isPermanent ? -1L : currentDuration;
                if (editIndex == null) {
                    if (!parent.addRestriction(type, duration)) {
                        player.sendMessage(CC.errorMsg("That restriction type is already selected."));
                        return;
                    }
                } else {
                    parent.updateRestriction(editIndex, duration);
                }
                parent.openMenu(player);
            }
        }

        private class ClearButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.BARRIER)
                        .setDisplayName(CC.format("<red>Clear"))
                        .setLore(CC.format("<gray>Reset duration to 0"))
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                currentDuration      = 0L;
                isPermanent          = false;
                selectedStepIndexes  = new int[]{0, 0, 0, 0};
                openMenu(player);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Main menu buttons
    // ══════════════════════════════════════════════════════════════════════════

    private class InfoButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.BOOK)
                    .setDisplayName(CC.format("<dark_purple><bold>Punishment Builder"))
                    .setLore(
                            CC.format("<gray>Violation: <light_purple>" + infractionType.getDisplayName()),
                            CC.format("<gray>Target: <light_purple>" + target.getName()),
                            CC.format("<gray>Restrictions: <light_purple>" + selectedActions.size()),
                            CC.format(""),
                            CC.format("<light_purple>Recommended actions are preloaded."),
                            CC.format("<light_purple>Add or edit durations before confirming.")
                    )
                    .build();
        }
    }

    private class SelectedActionButton extends Button {
        private final int index;
        private final RestrictionAction action;

        SelectedActionButton(int index, RestrictionAction action) {
            this.index  = index;
            this.action = action;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(getMaterial(action.getType()))
                    .setDisplayName(CC.format("<dark_purple>" + action.getType().getName()))
                    .setLore(
                            CC.format("<gray>Length: <light_purple>" + formatDuration(action.getDuration())),
                            CC.format(""),
                            CC.format("<yellow>Left-click<gray> to change duration"),
                            CC.format("<yellow>Right-click<gray> to remove")
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
            new DurationSelectionMenu(
                    PunishActionMenu.this, action.getType(), index, PunishActionMenu.this, action.getDuration()
            ).openMenu(player);
        }
    }

    private class AddRestrictionButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            boolean full = selectedActions.size() >= ACTION_SLOTS.length;
            return new ItemBuilder(full ? Material.BARRIER : Material.ANVIL)
                    .setDisplayName(CC.format(full ? "<red>No more slots" : "<green>Add Restriction"))
                    .setLore(CC.format(full
                            ? "<gray>Maximum of " + ACTION_SLOTS.length + " restrictions reached."
                            : "<gray>Open a submenu to add a new restriction."))
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (selectedActions.size() >= ACTION_SLOTS.length) return;
            new RestrictionTypeMenu(PunishActionMenu.this).openMenu(player);
        }
    }

    private class ClearButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName(CC.format("<red>Clear All"))
                    .setLore(CC.format("<gray>Remove all selected restrictions."))
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
            boolean empty = selectedActions.isEmpty();
            List<Component> lore = new ArrayList<>();
            lore.add(CC.format("<gray>Issue all selected restrictions as one punishment."));
            if (empty) {
                lore.add(CC.format(""));
                lore.add(CC.format("<red>Add at least one restriction first."));
            } else {
                lore.add(CC.format(""));
                selectedActions.forEach(a ->
                        lore.add(CC.format("  <light_purple>" + a.getType().getName()
                                + " <dark_gray>- <gray>" + formatDuration(a.getDuration()))));
            }
            return new ItemBuilder(empty ? Material.GRAY_CONCRETE : Material.LIME_CONCRETE)
                    .setDisplayName(CC.format(empty ? "<gray>Confirm Punishment" : "<green>Confirm Punishment"))
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (selectedActions.isEmpty()) {
                player.sendMessage(CC.errorMsg("You need at least one restriction."));
                return;
            }
            if (hasDuplicateActionTypes()) {
                player.sendMessage(CC.errorMsg("Duplicate restriction types are not allowed."));
                return;
            }

            new ConfirmationMenu(
                    "Issue " + selectedActions.size() + " restriction(s) to " + target.getName() + "?",
                    confirmed -> {
                        if (!confirmed) return;
                        new PunishManager(player, target, getSelectedActions(), infractionType, message).issue();
                        player.closeInventory();
                    }
            ).openMenu(player);
        }
    }
}
