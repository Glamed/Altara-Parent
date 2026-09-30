package games.sparking.altara.review;

import games.sparking.altara.Altara;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every automated punishment still waiting on the review its own message promises the
 * player ("pending staff review"). Left-click acknowledges an entry — the restriction
 * stands exactly as the system applied it; shift-right-click revokes it outright. The
 * refresh button re-fetches the queue from the API so a second reviewer's changes show up.
 */
public class ReviewQueueMenu extends PagedMenu {

    private static final int REFRESH_SLOT = 45;
    private static final int EMPTY_SLOT   = 22;

    private List<Punishment> pending;

    public ReviewQueueMenu(List<Punishment> pending) {
        this.pending = new ArrayList<>(pending);
        this.pending.sort(Comparator.comparingLong(Punishment::getIssuedAt));
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Review Queue"};
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(REFRESH_SLOT, refreshButton());
        if (pending.isEmpty()) {
            buttons.put(EMPTY_SLOT, Button.createPlaceholder(new ItemBuilder(Material.LIME_DYE)
                    .setDisplayName(Component.text("Nothing pending", Theme.SUCCESS))
                    .setLore(Gui.lore().text("Every automated action has been reviewed.").build())
                    .build()));
        }
        return buttons;
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (Punishment punishment : pending) {
            buttons.put(buttons.size(), new PendingButton(punishment));
        }
        return buttons;
    }

    private Button refreshButton() {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.SUNFLOWER)
                        .setDisplayName(Component.text()
                                .append(Component.text("↻ ", Theme.PRIMARY_DARK))
                                .append(Component.text("Refresh", Theme.TEXT_STRONG))
                                .build())
                        .setLore(Gui.lore()
                                .text(pending.size() + " pending right now.")
                                .cta("refresh")
                                .build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                Tasks.runAsync(() -> {
                    List<Punishment> fresh = Altara.getSharedInstance().getPunishmentService().getPendingReview();
                    Tasks.run(() -> {
                        if (player.isOnline()) new ReviewQueueMenu(fresh).openMenu(player);
                    });
                });
            }
        };
    }

    private static String nameOf(String uuid) {
        if (uuid == null) return "Unknown";
        if (!UUIDUtils.isUUID(uuid)) return uuid;
        String name = UUIDCache.getName(UUID.fromString(uuid));
        return name != null ? name : "Unknown";
    }

    private static String lengthOf(Punishment punishment, RestrictionAction action) {
        if (action.isPermanent()) return "Permanent";
        if (action.hasExpired(punishment.getIssuedAt())) return "Expired";
        return Time.formatDetailed(Math.max(0L, punishment.getIssuedAt() + action.getDuration() - System.currentTimeMillis())) + " left";
    }

    private class PendingButton extends Button {

        private final Punishment punishment;

        private PendingButton(Punishment punishment) {
            this.punishment = punishment;
        }

        @Override
        public ItemStack getItem(Player player) {
            String playerName = nameOf(punishment.getPlayerUuid());

            Gui.Lore lore = Gui.lore()
                    .value("Player", playerName)
                    .value("Issued", Time.formatTimeAgo(punishment.getIssuedAt()));

            if (punishment.getMessage() != null && !punishment.getMessage().isBlank()) {
                lore.value("Trigger", punishment.getMessage());
            }

            if (punishment.getActions() != null && !punishment.getActions().isEmpty()) {
                lore.blank().heading("Restrictions applied");
                punishment.getActions().forEach(action ->
                        lore.entry(action.getType().getName() + " - " + lengthOf(punishment, action)));
            }

            lore.blank()
                    .line(Gui.cta("acknowledge"))
                    .line(Component.text("Shift + right click to revoke instead", Theme.TEXT));

            return new ItemBuilder(InfractionType.TEMP_AUTOMATED.getMaterial())
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName(Gui.name(playerName).append(Component.space())
                            .append(CC.bracketed(Component.text("Pending", Theme.WARNING))))
                    .setLore(lore.build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            String playerName = nameOf(punishment.getPlayerUuid());

            if (clickType == ClickType.SHIFT_RIGHT) {
                new ConfirmationMenu(new String[]{"Review Queue", "Revoke"}, "Revoke this automated action?",
                        List.of("Every restriction it applied to", playerName + " ends immediately."),
                        "Revoke", "Keep it", confirmed -> {
                            if (!confirmed) {
                                openMenu(player);
                                return;
                            }
                            Tasks.runAsync(() -> {
                                boolean ok = Altara.getSharedInstance().getPunishmentService()
                                        .revokePunishment(punishment.getId(), player.getUniqueId());
                                player.sendMessage(ok
                                        ? CC.success("Automated action revoked.", "*" + playerName + "* is no longer restricted by it.")
                                        : CC.error("Unable to revoke.", "It may already have been handled."));
                                removeAndReopen(player, ok);
                            });
                        }).openMenu(player);
                return;
            }

            Tasks.runAsync(() -> {
                boolean ok = Altara.getSharedInstance().getPunishmentService()
                        .markReviewed(punishment.getId(), player.getUniqueId());
                player.sendMessage(ok
                        ? CC.success("Marked as reviewed.", "*" + playerName + "*'s automated action stands as applied.")
                        : CC.error("Unable to update.", "It may already have been handled."));
                removeAndReopen(player, ok);
            });
        }

        private void removeAndReopen(Player player, boolean handled) {
            if (handled) pending.remove(punishment);
            Tasks.run(() -> {
                if (player.isOnline()) openMenu(player);
            });
        }
    }
}
