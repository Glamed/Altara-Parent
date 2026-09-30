package games.sparking.altara.report.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.punishment.PunishTarget;
import games.sparking.altara.punishment.menu.PunishMenu;
import games.sparking.altara.report.ReportMessage;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import games.sparking.altara.uuid.UUIDCache;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chat history attached to a report, newest first. Left click to punish, right click to see recipients. */
public class ChatReportMenu extends PagedMenu {

    private final String reportId;
    private final List<ReportMessage> messages;

    public ChatReportMenu(String reportId, List<ReportMessage> messages) {
        this.reportId = reportId;
        this.messages = messages;
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Report", "Chat History"};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int index = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            buttons.put(index++, new MessageButton(messages.get(i)));
        }
        return buttons;
    }

    @RequiredArgsConstructor
    private class MessageButton extends Button {

        private final ReportMessage message;

        @Override
        public ItemStack getItem(Player player) {
            String senderName = UUIDCache.getName(UUID.fromString(message.getSenderUuid()));
            if (senderName == null) senderName = message.getSenderUuid();

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(Bukkit.getOfflinePlayer(UUID.fromString(message.getSenderUuid())))
                    .setDisplayName(Component.text(senderName, Theme.PRIMARY))
                    .setLore(Gui.lore()
                            .value("Sent", Time.formatDetailed(System.currentTimeMillis() - message.getSentAt()) + " ago")
                            .heading("Message")
                            .entry(message.getMessage())
                            .blank()
                            .line(Component.text("Right click for recipients", Theme.TEXT))
                            .line(Component.text("Left click to punish", Theme.ERROR))
                            .build())
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            UUID senderUuid = UUID.fromString(message.getSenderUuid());
            if (clickType == ClickType.RIGHT) {
                new ChatReportRecipientsMenu(reportId, message).openMenu(whoClicked);
            } else if (clickType == ClickType.LEFT) {
                String senderName = UUIDCache.getName(senderUuid);
                new PunishMenu(new PunishTarget(senderUuid, senderName != null ? senderName : senderUuid.toString()),
                        message.getMessage()).openMenu(whoClicked);
            }
        }
    }
}
