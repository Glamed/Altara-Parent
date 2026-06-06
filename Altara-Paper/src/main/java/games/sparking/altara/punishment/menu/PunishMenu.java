package games.sparking.altara.punishment.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Messages;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
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

public class PunishMenu extends Menu {

    private final OfflinePlayer target;
    private final String message;

    public PunishMenu(OfflinePlayer target) {
        this(target, null);
    }

    public PunishMenu(OfflinePlayer target, String message) {
        this.target = target;
        this.message = message;
    }

    @Override
    public Component getTitle(Player player) {
        return CC.format("<gray>Punish <light_purple>" + target.getName());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4,  new HeadButton(target));
        buttons.put(49, new ModifyPunishmentsButton());

        InfractionType[] visible = InfractionType.visibleValues();
        int total = visible.length;
        int index = 19;

        for (int i = 0; i < total; ) {
            int itemsThisRow = Math.min(7, total - i);
            int rowStart     = (index / 7) * 9;
            int startOffset  = (9 - itemsThisRow) / 2;

            for (int j = 0; j < itemsThisRow; j++, i++) {
                buttons.put(rowStart + startOffset + j, new TypeButton(visible[i]));
            }

            index += 7;
        }

        return buttons;
    }

    // ── Buttons ────────────────────────────────────────────────────────────────

    @RequiredArgsConstructor
    public class HeadButton extends Button {

        private final OfflinePlayer p;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(p.getName())
                    .setDisplayName(CC.format("<gray>Punish <light_purple>" + p.getName()))
                    .build();
        }
    }

    public class TypeButton extends Button {

        private final InfractionType infractionType;

        public TypeButton(InfractionType infractionType) {
            this.infractionType = infractionType;
        }

        @Override
        public ItemStack getItem(Player player) {
            // Word-wrap the description at ~40 chars per line.
            List<Component> lore = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : infractionType.getDescription().split(" ")) {
                if (line.length() + word.length() + 1 > 40) {
                    lore.add(CC.format("<gray><italic>" + line));
                    line = new StringBuilder(word);
                } else {
                    if (!line.isEmpty()) line.append(" ");
                    line.append(word);
                }
            }
            if (!line.isEmpty()) lore.add(CC.format("<gray><italic>" + line));

            return new ItemBuilder(infractionType.getMaterial())
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName(CC.format("<dark_purple><bold>" + infractionType.getDisplayName()))
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget == null) {
                whoClicked.sendMessage(CC.errorMsg(Messages.CONNECTED));
                return;
            }

            new PunishActionMenu()
                    .initialize(onlineTarget, infractionType, message, PunishMenu.this)
                    .openMenu(whoClicked);
        }
    }

    public class ModifyPunishmentsButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName(CC.format("<light_purple>Modify Existing Punishments"))
                    .setLore(
                            CC.format("<gray>View punishment history and edit"),
                            CC.format("<gray>active actions, timespans, and reasons.")
                    )
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            new PunishmentModifyMenu(target, PunishMenu.this).openMenu(whoClicked);
        }
    }
}