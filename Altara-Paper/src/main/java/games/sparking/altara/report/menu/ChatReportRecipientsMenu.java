package games.sparking.altara.report.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.report.ReportMessage;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.uuid.UUIDCache;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Who received one specific cached chat message, and whether they reported it. */
public class ChatReportRecipientsMenu extends games.sparking.altara.menu.page.PagedMenu {

    private final String reportId;
    private final ReportMessage message;

    public ChatReportRecipientsMenu(String reportId, ReportMessage message) {
        this.reportId = reportId;
        this.message = message;
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Report", "Recipients"};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int index = 0;

        List<String> reporters = new ArrayList<>(message.getReportedBy());
        for (String uuid : reporters) {
            buttons.put(index++, new RecipientButton(uuid, true));
        }
        for (String uuid : message.getRecipients()) {
            if (!reporters.contains(uuid)) {
                buttons.put(index++, new RecipientButton(uuid, false));
            }
        }
        return buttons;
    }

    @RequiredArgsConstructor
    private class RecipientButton extends Button {

        private final String uuidString;
        private final boolean reported;

        @Override
        public ItemStack getItem(Player player) {
            UUID uuid = UUID.fromString(uuidString);
            String name = UUIDCache.getName(uuid);
            if (name == null) name = uuidString;

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(Bukkit.getOfflinePlayer(uuid))
                    .setDisplayName(Component.text(name, reported ? Theme.PRIMARY : Theme.TEXT_STRONG))
                    .setLore(reported
                            ? List.of(Component.text("Reported this message", Theme.SUCCESS))
                            : List.of())
                    .build();
        }
    }
}
