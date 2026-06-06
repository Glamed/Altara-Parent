package games.sparking.altara.punishment.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.buttons.BackButton;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.punishment.*;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PunishmentModifyMenu extends PagedMenu {

    private final OfflinePlayer target;
    private final Menu backMenu;

    public PunishmentModifyMenu(OfflinePlayer target) {
        this(target, new PunishMenu(target));
    }

    public PunishmentModifyMenu(OfflinePlayer target, Menu backMenu) {
        this.target   = target;
        this.backMenu = backMenu;
    }

    // ── PagedMenu contract ─────────────────────────────────────────────────────

    @Override
    public String getRawTitle(Player player) {
        return "Modify " + getTargetName() + " punishments";
    }

    @Override public int getSize()            { return 54; }
    @Override public int getMaxItemsPerPage() { return 36; }
    @Override public boolean isAutoUpdate()   { return false; }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4,  new TargetInfoButton());
        buttons.put(49, new BackButton(backMenu));

        if (punishments().isEmpty()) {
            buttons.put(22, new EmptyButton());
        }

        return buttons;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<Punishment> list = punishments();
        for (int i = 0; i < list.size(); i++) {
            buttons.put(i, new PunishmentButton(list.get(i)));
        }
        return buttons;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<Punishment> punishments() {
        if (target.getUniqueId() == null) return List.of();
        return Altara.getSharedInstance().getPunishmentService().getPunishments(target.getUniqueId());
    }

    private Material getMaterial(PunishmentType type) {
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

    private String getTargetName() {
        return target.getName() == null ? "Unknown" : target.getName();
    }

    // ── Buttons ────────────────────────────────────────────────────────────────

    private class TargetInfoButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(getTargetName())
                    .setDisplayName(CC.format("<dark_purple><bold>Modify Punishments"))
                    .setLore(
                            CC.format("<gray>Target: <light_purple>" + getTargetName()),
                            CC.format("<gray>Left-click to view details."),
                            CC.format("<gray>Shift-right-click to revoke.")
                    )
                    .build();
        }
    }

    private static class EmptyButton extends Button {
        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName(CC.format("<red>No punishments found"))
                    .setLore(CC.format("<gray>This player has no punishment history."))
                    .build();
        }
    }

    private class PunishmentButton extends Button {

        private final Punishment punishment;

        private PunishmentButton(Punishment punishment) {
            this.punishment = punishment;
        }

        @Override
        public ItemStack getItem(Player player) {
            boolean active      = punishment.isActive();
            String  status      = active ? "<green>ACTIVE" : "<red>INACTIVE";
            String  reasonName  = punishment.getReason() != null
                                  ? punishment.getReason().getDisplayName() : "Unknown";
            String  staffName   = punishment.getStaffUUID() != null
                                  ? Bukkit.getOfflinePlayer(punishment.getStaffUUID()).getName() : "Console";

            List<Component> lore = new ArrayList<>();
            lore.add(CC.format("<gray>Status: " + status));
            lore.add(CC.format("<gray>Reason: <light_purple>" + reasonName));
            lore.add(CC.format("<gray>Issued: <light_purple>" + Time.formatDate(punishment.getIssuedAt())));
            lore.add(CC.format("<gray>Staff: <light_purple>" + (staffName == null ? "Unknown" : staffName)));

            if (punishment.getMessage() != null && !punishment.getMessage().isEmpty()) {
                lore.add(CC.format(""));
                lore.add(CC.format("<gray>Message: <white>" + punishment.getMessage()));
            }

            // List each individual restriction action.
            if (punishment.getActions() != null && !punishment.getActions().isEmpty()) {
                lore.add(CC.format(""));
                lore.add(CC.format("<gray>Restrictions:"));
                for (RestrictionAction action : punishment.getActions()) {
                    boolean actionActive  = !action.hasExpired(punishment.getIssuedAt());
                    String  durationLabel = action.isPermanent() ? "Permanent"
                            : actionActive
                                    ? Time.formatDetailed(Math.max(0L,
                                            (punishment.getIssuedAt() + action.getDuration()) - System.currentTimeMillis())) + " remaining"
                                    : "Expired";
                    lore.add(CC.format(
                            "  " + (actionActive ? "<green>" : "<gray>") + action.getType().getName()
                            + " <dark_gray>- <light_purple>" + durationLabel));
                }
            }

            lore.add(CC.format(""));
            if (active) {
                lore.add(CC.format("<yellow>Left-click<gray>: view detail"));
                lore.add(CC.format("<yellow>Shift-right<gray>: revoke"));
            } else {
                lore.add(CC.format("<gray>Inactive punishments are read-only."));
            }

            // Use the primary (first) action type for the icon.
            PunishmentType iconType = punishment.getPrimaryType();
            return new ItemBuilder(getMaterial(iconType))
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName(CC.format("<dark_purple>" + (iconType != null ? iconType.getName() : "Punishment")))
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (clickType == ClickType.SHIFT_RIGHT) {
                if (!punishment.isActive()) {
                    player.sendMessage(CC.errorMsg("This punishment is already inactive."));
                    return;
                }

                new ConfirmationMenu(
                        "Revoke this punishment?",
                        "Revoke",
                        "Keep Active",
                        confirmed -> {
                            if (!confirmed) {
                                new PunishmentModifyMenu(target, backMenu).openMenu(player);
                                return;
                            }

                            boolean ok = Altara.getSharedInstance().getPunishmentService()
                                    .revokePunishment(punishment.getId(), player.getUniqueId());
                            player.sendMessage(ok
                                    ? CC.successMsg("Punishment revoked.")
                                    : CC.errorMsg("Could not revoke. It may have already been removed."));
                            new PunishmentModifyMenu(target, backMenu).openMenu(player);
                        }
                ).openMenu(player);
                return;
            }

            // Left-click: open a detail / info menu (read-only)
            new PunishmentDetailMenu(punishment).openMenu(player);
        }
    }

    // ── Detail menu (read-only) ────────────────────────────────────────────────

    private class PunishmentDetailMenu extends Menu {

        private final Punishment punishment;

        private PunishmentDetailMenu(Punishment punishment) {
            this.punishment = punishment;
        }

        @Override
        public Component getTitle(Player player) {
            String type = punishment.getPrimaryType() != null
                          ? punishment.getPrimaryType().getName() : "Punishment";
            return net.kyori.adventure.text.Component.text("Punishment: " + type);
        }

        @Override
        public int getSize() { return 27; }

        @Override
        public Map<Integer, Button> getButtons(Player player) {
            Map<Integer, Button> buttons = new HashMap<>();
            buttons.put(4,  new DetailInfoButton());
            buttons.put(18, new BackButton(new PunishmentModifyMenu(target, backMenu)));

            int slot = 10;
            if (punishment.getActions() != null) {
                for (RestrictionAction action : punishment.getActions()) {
                    if (slot > 16) break;
                    buttons.put(slot++, new ActionDisplayButton(action));
                }
            }

            if (punishment.isActive()) {
                buttons.put(22, new RevokeButton());
            }

            return buttons;
        }

        private class DetailInfoButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                String reasonName = punishment.getReason() != null
                                    ? punishment.getReason().getDisplayName() : "Unknown";
                return new ItemBuilder(Material.BOOK)
                        .setDisplayName(CC.format("<dark_purple><bold>Punishment Detail"))
                        .setLore(
                                CC.format("<gray>Reason: <light_purple>" + reasonName),
                                CC.format("<gray>Issued: <light_purple>" + Time.formatDate(punishment.getIssuedAt())),
                                CC.format("<gray>Status: " + (punishment.isActive() ? "<green>ACTIVE" : "<red>INACTIVE"))
                        )
                        .build();
            }
        }

        private class ActionDisplayButton extends Button {
            private final RestrictionAction action;

            ActionDisplayButton(RestrictionAction action) {
                this.action = action;
            }

            @Override
            public ItemStack getItem(Player player) {
                boolean active  = !action.hasExpired(punishment.getIssuedAt());
                String  dLabel  = action.isPermanent() ? "Permanent"
                        : active
                                ? Time.formatDetailed(Math.max(0L,
                                        (punishment.getIssuedAt() + action.getDuration()) - System.currentTimeMillis())) + " remaining"
                                : "Expired";
                return new ItemBuilder(getMaterial(action.getType()))
                        .setDisplayName(CC.format((active ? "<green>" : "<gray>") + action.getType().getName()))
                        .setLore(CC.format("<gray>Duration: <light_purple>" + dLabel))
                        .build();
            }
        }

        private class RevokeButton extends Button {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.RED_WOOL)
                        .setDisplayName(CC.format("<red>Revoke Punishment"))
                        .setLore(CC.format("<gray>Deactivates all restrictions."))
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                new ConfirmationMenu(
                        "Revoke this punishment?",
                        "Revoke",
                        "Cancel",
                        confirmed -> {
                            if (!confirmed) {
                                new PunishmentDetailMenu(punishment).openMenu(player);
                                return;
                            }
                            boolean ok = Altara.getSharedInstance().getPunishmentService()
                                    .revokePunishment(punishment.getId(), player.getUniqueId());
                            player.sendMessage(ok
                                    ? CC.successMsg("Punishment revoked.")
                                    : CC.errorMsg("Could not revoke."));
                            new PunishmentModifyMenu(target, backMenu).openMenu(player);
                        }
                ).openMenu(player);
            }
        }
    }
}
