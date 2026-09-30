package games.sparking.altara.punishment.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.punishment.*;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Builds the set of restrictions for one punishment.  The recommended actions for the
 * violation — escalated by the player's recent history — are preloaded.
 */
public class PunishActionMenu extends Menu {

    private static final int[] ACTION_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final List<RestrictionAction> selectedActions = new ArrayList<>();
    private final PunishTarget target;
    private final InfractionType infractionType;
    private final String message;
    private final Menu backMenu;
    private final AccountStatus status;

    public PunishActionMenu(PunishTarget target, InfractionType infractionType, String message, Menu backMenu) {
        this.target = target;
        this.infractionType = infractionType;
        this.message = message;
        this.backMenu = backMenu;

        // History is loaded at login for online players; offline targets fall back to a clean record.
        List<Punishment> history = Altara.getSharedInstance().getPunishmentService().getCachedPunishments(target.uuid());
        this.status = history == null ? AccountStatus.CLEAN : AccountStatus.compute(history);

        for (RestrictionAction action : infractionType.getRecommendedActions(status)) {
            if (!hasActionType(action.getType())) selectedActions.add(action);
        }
    }

    boolean addRestriction(PunishmentType type, long duration) {
        if (hasActionType(type)) return false;
        selectedActions.add(new RestrictionAction(type, duration));
        return true;
    }

    void updateRestriction(int index, long duration) {
        RestrictionAction old = selectedActions.get(index);
        selectedActions.set(index, new RestrictionAction(old.getType(), duration));
    }

    boolean hasActionType(PunishmentType type) {
        return selectedActions.stream().anyMatch(action -> action.getType() == type);
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Punish", target.name());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4, new InfoButton());
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());
        buttons.put(45, Gui.backButton("Violations", () -> backMenu));
        buttons.put(47, new ClearButton());
        buttons.put(49, new ConfirmButton());
        buttons.put(51, new AddRestrictionButton());

        for (int i = 0; i < Math.min(ACTION_SLOTS.length, selectedActions.size()); i++) {
            buttons.put(ACTION_SLOTS[i], new SelectedActionButton(i, selectedActions.get(i)));
        }
        return buttons;
    }

    static Material materialOf(PunishmentType type) {
        if (type == null) return Material.PAPER;
        return switch (type) {
            case SUSPENSION          -> Material.IRON_BARS;
            case CHAT_RESTRICTION    -> Material.PAPER;
            case DISCORD_RESTRICTION -> Material.BOOK;
            case COMP_GAMEPLAY       -> Material.DIAMOND_SWORD;
            case REPORT              -> Material.WRITABLE_BOOK;
            case WARN                -> Material.YELLOW_DYE;
        };
    }

    static String formatDuration(long duration) {
        if (duration == -1) return "Permanent";
        if (duration <= 0) return "Immediate";
        return Time.formatDetailed(duration);
    }

    // ── Main buttons ─────────────────────────────────────────────────────────

    private class InfoButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(infractionType.getMaterial())
                    .setDisplayName(Gui.name(infractionType.getDisplayName()))
                    .setLore(Gui.lore()
                            .value("Player", target.name())
                            .value("Standing", status.getLevel().getDisplayName())
                            .value("Restrictions", String.valueOf(selectedActions.size()))
                            .blank()
                            .text("Recommended actions are preloaded", "for this player's standing.")
                            .build())
                    .build();
        }
    }

    private class SelectedActionButton extends Button {
        private final int index;
        private final RestrictionAction action;

        SelectedActionButton(int index, RestrictionAction action) {
            this.index = index;
            this.action = action;
        }

        @Override
        public ItemStack getItem(Player player) {
            Gui.Lore lore = Gui.lore().value("Length", formatDuration(action.getDuration())).blank();
            if (action.getType() != PunishmentType.WARN) {
                lore.line(Gui.cta("change the length").append(Component.text(" (left)", Theme.TEXT)));
            }
            lore.line(Gui.cta("remove it").append(Component.text(" (right)", Theme.TEXT)));

            return new ItemBuilder(materialOf(action.getType()))
                    .setDisplayName(Gui.name(action.getType().getName()))
                    .setLore(lore.build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (clickType.isRightClick()) {
                selectedActions.remove(index);
                openMenu(player);
                return;
            }
            if (action.getType() == PunishmentType.WARN) return;
            new DurationMenu(PunishActionMenu.this, action.getType(), index, action.getDuration()).openMenu(player);
        }
    }

    private class AddRestrictionButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            if (selectedActions.size() >= ACTION_SLOTS.length) {
                return new ItemBuilder(Material.GRAY_DYE)
                        .setDisplayName(Component.text(Theme.CROSS + " ", Theme.TEXT).append(Component.text("Add Restriction", Theme.ERROR)))
                        .setLore(Gui.lore().text("No more restrictions can be added.").build())
                        .build();
            }
            return new ItemBuilder(Material.ANVIL)
                    .setDisplayName(Gui.name("Add", "Restriction"))
                    .setLore(Gui.lore().text("Add another restriction", "to this punishment.").cta("choose a restriction").build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (selectedActions.size() < ACTION_SLOTS.length) new RestrictionTypeMenu(PunishActionMenu.this).openMenu(player);
        }
    }

    private class ClearButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.LAVA_BUCKET)
                    .setDisplayName(Gui.name("Clear", "Restrictions"))
                    .setLore(Gui.lore().text("Remove every selected restriction.").cta("clear the list").build())
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
            if (selectedActions.isEmpty()) {
                return new ItemBuilder(Material.GRAY_WOOL)
                        .setDisplayName(Component.text(Theme.CROSS + " ", Theme.TEXT).append(Component.text("Issue Punishment", Theme.ERROR)))
                        .setLore(Gui.lore().text("Add at least one restriction first.").build())
                        .build();
            }

            Gui.Lore lore = Gui.lore().heading("Restrictions");
            selectedActions.forEach(a -> lore.entry(a.getType().getName() + " - " + formatDuration(a.getDuration())));
            return Gui.acceptItem("Issue Punishment", lore.cta("review and confirm").build());
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (selectedActions.isEmpty()) return;

            List<String> details = new ArrayList<>();
            details.add("Violation: " + infractionType.getDisplayName());
            selectedActions.forEach(a -> details.add("- " + a.getType().getName() + ": " + formatDuration(a.getDuration())));

            new ConfirmationMenu(new String[]{"Punish", "Confirm"},
                    "Punish " + target.name() + "?", details, "Issue", "Go back",
                    confirmed -> {
                        if (confirmed) new PunishManager(player, target, selectedActions, infractionType, message).issue();
                        else openMenu(player);
                    }).openMenu(player);
        }
    }

    // ── Restriction type picker ──────────────────────────────────────────────

    private static class RestrictionTypeMenu extends Menu {

        private static final int[] SLOTS = {10, 11, 12, 14, 15, 16};
        private static final PunishmentType[] TYPES = {
                PunishmentType.WARN, PunishmentType.CHAT_RESTRICTION, PunishmentType.DISCORD_RESTRICTION,
                PunishmentType.REPORT, PunishmentType.COMP_GAMEPLAY, PunishmentType.SUSPENSION
        };

        private final PunishActionMenu parent;

        private RestrictionTypeMenu(PunishActionMenu parent) {
            this.parent = parent;
        }

        @Override
        public Component getTitle(Player player) {
            return Gui.title("Punish", "Add Restriction");
        }

        @Override public int getSize() { return 27; }
        @Override public FillTemplate getFillTemplate() { return FillTemplate.ALTARA; }
        @Override public boolean isAutoUpdate() { return false; }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            for (int i = 0; i < TYPES.length; i++) buttons.put(SLOTS[i], new TypeChoiceButton(TYPES[i]));
            buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());
            buttons.put(18, Gui.backButton("Restrictions", () -> parent));
            return buttons;
        }

        private class TypeChoiceButton extends Button {
            private final PunishmentType type;

            private TypeChoiceButton(PunishmentType type) {
                this.type = type;
            }

            @Override
            public ItemStack getItem(Player player) {
                if (parent.hasActionType(type)) {
                    return new ItemBuilder(materialOf(type))
                            .setDisplayName(Component.text(type.getName(), Theme.TEXT))
                            .setLore(Gui.lore().text("Already added.").build())
                            .build();
                }
                return new ItemBuilder(materialOf(type))
                        .setDisplayName(Gui.name(type.getName()))
                        .setLore(Gui.lore().cta(type == PunishmentType.WARN ? "add a warning" : "choose a length").build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                if (parent.hasActionType(type)) return;
                if (type == PunishmentType.WARN) {
                    parent.addRestriction(type, 0);
                    parent.openMenu(player);
                    return;
                }
                new DurationMenu(parent, type, null, 0L).openMenu(player);
            }
        }
    }

    // ── Duration picker ──────────────────────────────────────────────────────

    /** Step-based duration picker: one row each for minutes, hours, days and months. */
    private static class DurationMenu extends Menu {

        private static final String[] UNITS = {"Minutes", "Hours", "Days", "Months"};
        private static final long MINUTE = 60_000L, HOUR = 60 * MINUTE, DAY = 24 * HOUR, MONTH = 30 * DAY;
        private static final long[][] STEPS = {
                {MINUTE, 5 * MINUTE, 10 * MINUTE, 15 * MINUTE, 30 * MINUTE},
                {HOUR, 2 * HOUR, 3 * HOUR, 6 * HOUR, 12 * HOUR},
                {DAY, 3 * DAY, 7 * DAY, 14 * DAY},
                {MONTH, 2 * MONTH, 3 * MONTH, 6 * MONTH}
        };
        private static final int[] ROW_STARTS = {11, 20, 29, 38};
        private static final long[] PRESETS = {15 * MINUTE, HOUR, 6 * HOUR, DAY, 7 * DAY, 30 * DAY};
        private static final int[] PRESET_SLOTS = {46, 47, 48, 50, 51, 52};

        private final PunishActionMenu parent;
        private final PunishmentType type;
        private final Integer editIndex;

        private long duration;
        private boolean permanent;
        private final int[] stepIndex = {0, 0, 0, 0};

        private DurationMenu(PunishActionMenu parent, PunishmentType type, Integer editIndex, long initialDuration) {
            this.parent = parent;
            this.type = type;
            this.editIndex = editIndex;
            this.permanent = initialDuration == -1L;
            this.duration = Math.max(0L, initialDuration);
        }

        @Override
        public Component getTitle(Player player) {
            return Gui.title("Punish", type.getName());
        }

        @Override public int getSize() { return 54; }
        @Override public FillTemplate getFillTemplate() { return FillTemplate.ALTARA; }
        @Override public boolean isAutoUpdate() { return false; }
        @Override public boolean isClickUpdate() { return true; }

        private String label() {
            return permanent ? "Permanent" : duration == 0 ? "Not set" : Time.formatDetailed(duration);
        }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            buttons.put(4, Button.createPlaceholder(new ItemBuilder(Material.CLOCK)
                    .setDisplayName(Gui.name("Length"))
                    .setLore(Gui.lore().value("Current", label()).build())
                    .build()));
            buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());
            buttons.put(45, Gui.backButton("Restrictions", () -> parent));

            for (int row = 0; row < UNITS.length; row++) {
                int base = ROW_STARTS[row];
                buttons.put(base, new StepButton(row, -1));
                buttons.put(base + 1, new AdjustButton(row, false));
                buttons.put(base + 2, new UnitButton(row));
                buttons.put(base + 3, new AdjustButton(row, true));
                buttons.put(base + 4, new StepButton(row, 1));
            }

            for (int i = 0; i < PRESETS.length; i++) buttons.put(PRESET_SLOTS[i], new PresetButton(PRESETS[i]));
            buttons.put(49, new ConfirmButton());
            buttons.put(53, new PermanentButton());
            return buttons;
        }

        private class UnitButton extends Button {
            private final int row;

            UnitButton(int row) {
                this.row = row;
            }

            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.PAPER)
                        .setDisplayName(Gui.name(UNITS[row]))
                        .setLore(Gui.lore()
                                .value("Step", Time.formatDetailed(STEPS[row][stepIndex[row]]))
                                .value("Total", label())
                                .build())
                        .build();
            }
        }

        /** Changes the step size for a row. */
        private class StepButton extends Button {
            private final int row;
            private final int direction;

            StepButton(int row, int direction) {
                this.row = row;
                this.direction = direction;
            }

            @Override
            public ItemStack getItem(Player player) {
                int next = stepIndex[row] + direction;
                boolean available = next >= 0 && next < STEPS[row].length;
                Gui.Lore lore = Gui.lore().value("Step", Time.formatDetailed(STEPS[row][stepIndex[row]]));
                if (available) lore.cta((direction < 0 ? "use a smaller step: " : "use a larger step: ") + Time.formatDetailed(STEPS[row][next]));
                else lore.text(direction < 0 ? "This is the smallest step." : "This is the largest step.");

                return new ItemBuilder(Material.ARROW)
                        .setDisplayName(direction < 0
                                ? Component.text(Theme.BACK + " ", Theme.TEXT).append(Component.text("Smaller step", Theme.TEXT_STRONG))
                                : Component.text("Larger step", Theme.TEXT_STRONG).append(Component.text(" ❱", Theme.TEXT)))
                        .setLore(lore.build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                int next = stepIndex[row] + direction;
                if (next >= 0 && next < STEPS[row].length) stepIndex[row] = next;
            }
        }

        private class AdjustButton extends Button {
            private final int row;
            private final boolean increase;

            AdjustButton(int row, boolean increase) {
                this.row = row;
                this.increase = increase;
            }

            @Override
            public ItemStack getItem(Player player) {
                String step = Time.formatDetailed(STEPS[row][stepIndex[row]]);
                return new ItemBuilder(increase ? Material.LIME_DYE : Material.RED_DYE)
                        .setDisplayName(Component.text((increase ? "+ " : "- ") + step, increase ? Theme.SUCCESS : Theme.ERROR))
                        .setLore(Gui.lore().value("Total", label()).cta(increase ? "add " + step : "remove " + step).build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                long step = STEPS[row][stepIndex[row]];
                permanent = false;
                duration = Math.max(0, duration + (increase ? step : -step));
            }
        }

        private class PresetButton extends Button {
            private final long preset;

            PresetButton(long preset) {
                this.preset = preset;
            }

            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.CLOCK)
                        .setDisplayName(Gui.name(Time.formatDetailed(preset)))
                        .setLore(Gui.lore().cta("use this length").build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                duration = preset;
                permanent = false;
            }
        }

        private class PermanentButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Gui.settingMaterial(permanent))
                        .setDisplayName(Gui.settingName("Permanent", permanent))
                        .setLore(Gui.lore().text("Permanent restrictions never expire.").cta(permanent ? "disable" : "enable").build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                permanent = !permanent;
            }
        }

        private class ConfirmButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                String error = PunishmentModifyValidator.validateDuration(type, permanent ? -1L : duration);
                if (error != null) {
                    return new ItemBuilder(Material.GRAY_WOOL)
                            .setDisplayName(Component.text(Theme.CROSS + " ", Theme.TEXT).append(Component.text("Confirm Length", Theme.ERROR)))
                            .setLore(Gui.lore().text(error).build())
                            .build();
                }
                return Gui.acceptItem("Confirm Length", Gui.lore().value("Length", label()).cta("use this length").build());
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                long chosen = permanent ? -1L : duration;
                String error = PunishmentModifyValidator.validateDuration(type, chosen);
                if (error != null) {
                    player.sendMessage(CC.error("Invalid length.", error));
                    return;
                }

                if (editIndex == null) parent.addRestriction(type, chosen);
                else parent.updateRestriction(editIndex, chosen);
                parent.openMenu(player);
            }
        }
    }
}
