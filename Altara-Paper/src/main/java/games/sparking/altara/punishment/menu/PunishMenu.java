package games.sparking.altara.punishment.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
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
        return CC.format("Punish " + target.getName());
    }

    @Override
    public int getSize() {
        return 54;
    }


    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4, new HeadButton(target));


//        int index = 20;
//        for (games.sparking.crystalguard.punish.InfractionType types : games.sparking.crystalguard.punish.InfractionType.values()) {
//            buttons.put(index, new TypeButton(types, (Player) target));
//            if (++index % 9 == 7) {
//                index += 4;
//            }
//        }
//        int index = 19;
//        for (InfractionType types : InfractionType.values()) {
//            buttons.put(index, new TypeButton(types, (Player) target));
//            if (++index % 9 == 8) {
//                index += 2;
//            }
//        }
        int total = InfractionType.visibleValues().length;
        int index = 19;

        for (int i = 0; i < total; ) {
            int itemsThisRow = Math.min(7, total - i);
            int rowStart = (index / 7) * 9;
            int startOffset = (9 - itemsThisRow) / 2;

            for (int j = 0; j < itemsThisRow; j++, i++) {
                int slot = rowStart + startOffset + j;
                buttons.put(slot, new TypeButton(InfractionType.visibleValues()[i]));
            }

            index += 7;
        }
        return buttons;
    }

    @RequiredArgsConstructor
    public class HeadButton extends Button {

        private final OfflinePlayer p;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(p.getName())
                    .setDisplayName(CC.format("<gray>Punish <purple>" + p.getName()))
                    .build();
        }
    }

    public class TypeButton extends Button {

        private final InfractionType infractionType;

        public TypeButton(InfractionType InfractionType) {
            this.infractionType = InfractionType;
        }

        @Override
        public ItemStack getItem(Player player) {
            String desc = infractionType.getDescription();
            List<Component> lore = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String w : desc.split(" ")) {
                if (line.length() + w.length() + 1 > 40) {
                    lore.add(CC.format("<gray><i>" + line));
                    line = new StringBuilder(w);
                } else {
                    if (!line.isEmpty()) line.append(" ");
                    line.append(w);
                }
            }
            if (!line.isEmpty())
                lore.add(CC.format("<gray><i>" + line));

            return new ItemBuilder(infractionType.getMaterial())
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName(CC.format("<dark_purple><b>" + infractionType.getDisplayName()))
                    .setLore(lore)
                    .build();
        }


        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget == null) {
                whoClicked.sendMessage(ChatColor.RED + "That player is no longer online.");
                return;
            }

            new PunishActionMenu().initialize(onlineTarget, infractionType, message, PunishMenu.this).openMenu(whoClicked);
        }
    }
}