package games.sparking.altara.rank.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInputChain;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.setup.*;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
public class RankEditOverviewMenu extends Menu {

    private static final ChatInputChain SETUP_CHAIN = new ChatInputChain()
            .next(new NamePrompt())
            .next(new ColorPrompt())
            .next(new PrefixPrompt())
            .next(new WeightPrompt())
            .next(new QueuePriorityPrompt());

    private final Profile profile;

    @Override
    public String getTitle(Player player) {
        return "Rank Editor";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        Altara.getSharedInstance().getRankService().getRanksSorted().forEach(rank -> buttons.put(buttons.size(), new RankButton(rank)));
        buttons.put(buttons.size(), new SetupRankButton());
        return buttons;
    }

    @RequiredArgsConstructor
    public class RankButton extends Button {

        private final Rank rank;

        @Override
        public ItemStack getItem(Player player) {
            List<String> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);
            lore.add(CC.format("&eColor: %sExample", rank.getColor()));
            lore.add(CC.format("&eChat Color: %sExample", rank.getChatColor()));
            lore.add(" ");
            lore.add(CC.format("&ePrefix: %sExample", rank.getPrefix()));
            lore.add(CC.format("&eSuffix: &fExample%s", rank.getSuffix()));
            lore.add(" ");
            lore.add(CC.format("&eWeight: &c%d", rank.getWeight()));
            lore.add(CC.format("&eQueue Priority: &c%d", rank.getQueuePriority()));
            lore.add(" ");
            lore.add(CC.format("&eDefault: %s",
                    CC.colorBoolean(rank.isDefaultRank(), "true", "false")));
            lore.add(CC.format("&eDisguisable: %s",
                    CC.colorBoolean(rank.isDisguisable(), "true", "false")));
            lore.add(" ");
            lore.add(CC.format("&eInherits: &e%s", rank.getInherits().isEmpty() ? "None" : ""));
            if (!rank.getInherits().isEmpty()) {
                rank.getInherits().forEach(inherit -> lore.add(CC.GRAY + " - " + Altara.getSharedInstance().getRankService().getRank(inherit).getName()));
            }
            lore.add(" ");
            lore.add(CC.format("&ePermissions: &c%d", rank.getPermissions().size()));
            lore.add(CC.format("&eLocal Permissions: &c%d", rank.getLocalPermissions().size()));
            lore.add(CC.format("&eInherited Permissions: &c%d", rank.getInheritedPermissions().size()));
            lore.add(CC.MENU_BAR);

            return new ItemBuilder(rank.getMaterial())
                    .setDisplayName(rank.getName())
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            new RankEditingMenu(profile, rank).openMenu(player);
        }
    }

    public class SetupRankButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.EMERALD)
                    .setDisplayName(CC.GREEN + CC.BOLD + "Setup new rank")
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            player.getOpenInventory().close();
            SETUP_CHAIN.start(player);
        }
    }
}
