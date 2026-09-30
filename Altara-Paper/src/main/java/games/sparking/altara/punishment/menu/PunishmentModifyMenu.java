package games.sparking.altara.punishment.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.punishment.PunishTarget;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** A player's punishment history, newest first.  Shift-right-click revokes an active one. */
public class PunishmentModifyMenu extends PagedMenu {

    private final PunishTarget target;
    private final List<Punishment> history;
    private final Menu backMenu;

    public PunishmentModifyMenu(PunishTarget target, List<Punishment> history, Menu backMenu) {
        this.target = target;
        this.history = new ArrayList<>(history);
        this.history.sort(Comparator.comparingLong(Punishment::getIssuedAt).reversed());
        this.backMenu = backMenu;
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Punish", target.name(), "History"};
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(45, Gui.backButton("Violations", () -> backMenu));
        if (history.isEmpty()) {
            buttons.put(22, Button.createPlaceholder(new ItemBuilder(Material.PAPER)
                    .setDisplayName(Component.text("No punishments", Theme.TEXT_STRONG))
                    .setLore(Gui.lore().text(target.name() + " has a clean record.").build())
                    .build()));
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (Punishment punishment : history) {
            buttons.put(buttons.size(), new PunishmentButton(punishment));
        }
        return buttons;
    }

    private static String nameOf(String uuid) {
        if (uuid == null) return "Console";
        if (!UUIDUtils.isUUID(uuid)) return uuid;
        String name = UUIDCache.getName(UUID.fromString(uuid));
        return name != null ? name : "Unknown";
    }

    private static String lengthOf(Punishment punishment, RestrictionAction action) {
        if (action.isPermanent()) return "Permanent";
        if (action.getType() == PunishmentType.WARN) return "Warning";
        if (action.hasExpired(punishment.getIssuedAt())) return "Expired";
        return Time.formatDetailed(Math.max(0L, punishment.getIssuedAt() + action.getDuration() - System.currentTimeMillis())) + " left";
    }

    private class PunishmentButton extends Button {

        private final Punishment punishment;

        private PunishmentButton(Punishment punishment) {
            this.punishment = punishment;
        }

        @Override
        public ItemStack getItem(Player player) {
            boolean active = punishment.isActive();
            String state = punishment.isRemoved() ? "Revoked" : active ? "Active" : "Expired";
            String reason = punishment.getReason() != null ? punishment.getReason().getDisplayName() : "Unknown";

            Gui.Lore lore = Gui.lore()
                    .value("Issued by", nameOf(punishment.getStaffUUID() == null ? null : punishment.getStaffUUID().toString()))
                    .value("Issued on", Time.formatDate(punishment.getIssuedAt(), AltaraSettings.TIME_ZONE.get(player)));
            if (punishment.getMessage() != null && !punishment.getMessage().isBlank()) {
                lore.value("Message", punishment.getMessage());
            }

            if (punishment.getActions() != null && !punishment.getActions().isEmpty()) {
                lore.blank().heading("Restrictions");
                punishment.getActions().forEach(action -> lore.entry(action.getType().getName() + " - " + lengthOf(punishment, action)));
            }

            if (punishment.isRemoved()) {
                lore.blank().value("Revoked by", nameOf(punishment.getRemovedBy()));
            } else if (active) {
                lore.blank().line(Gui.cta("revoke it").append(Component.text(" (shift + right)", Theme.TEXT)));
            }

            return new ItemBuilder(PunishActionMenu.materialOf(punishment.getPrimaryType()))
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName(Gui.name(reason).append(Component.space())
                            .append(CC.bracketed(active ? Component.text(state, Theme.SUCCESS) : Component.text(state, Theme.TEXT))))
                    .setLore(lore.build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (clickType != ClickType.SHIFT_RIGHT || !punishment.isActive()) return;

            String reason = punishment.getReason() != null ? punishment.getReason().getDisplayName() : "this punishment";
            new ConfirmationMenu(new String[]{"Punish", "Revoke"}, "Revoke " + reason + "?",
                    List.of("Every restriction in this", "punishment ends immediately."),
                    "Revoke", "Keep it", confirmed -> {
                        if (!confirmed) {
                            openMenu(player);
                            return;
                        }
                        Tasks.runAsync(() -> {
                            boolean ok = Altara.getSharedInstance().getPunishmentService()
                                    .revokePunishment(punishment.getId(), player.getUniqueId());
                            player.sendMessage(ok
                                    ? CC.success("Punishment revoked.", "*" + target.name() + "*'s " + reason + " punishment is no longer active.")
                                    : CC.error("Unable to revoke.", "It may already have been revoked."));
                            Tasks.run(() -> {
                                if (player.isOnline()) openMenu(player);
                            });
                        });
                    }).openMenu(player);
        }
    }
}
