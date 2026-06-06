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
import net.kyori.adventure.text.Component;
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
    public Component getTitle(Player player) {
        return CC.format("Rank Editor");
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
            List<Component> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);
            lore.add(CC.format("<yellow>Color: %sExample", rank.getColor()));
            lore.add(CC.format("<yellow>Chat Color: %sExample", rank.getChatColor()));
            lore.add(CC.format(" "));
            lore.add(CC.format("<yellow>Prefix: %sExample", rank.getPrefix()));
            lore.add(CC.format("<yellow>Suffix: <white>Example%s", rank.getSuffix()));
            lore.add(CC.format(" "));
            lore.add(CC.format("<yellow>Weight: <red>%d", rank.getWeight()));
            lore.add(CC.format("<yellow>Queue Priority: <red>%d", rank.getQueuePriority()));
            lore.add(CC.format(" "));
            lore.add(CC.format("<yellow>Default: " + (rank.isDefaultRank() ? "<green>true" : "<red>false")));
            lore.add(CC.format("<yellow>Disguisable: " + (rank.isDisguisable() ? "<green>true" : "<red>false")));
            lore.add(CC.format(" "));
            lore.add(CC.format("<yellow>Inherits: %s", rank.getInherits().isEmpty() ? "None" : ""));
            if (!rank.getInherits().isEmpty()) {
                rank.getInherits().forEach(inherit -> lore.add(
                        CC.format("<gray> - " + Altara.getSharedInstance().getRankService().getRank(inherit.getUuid()).getName())));
            }
            lore.add(CC.format(" "));
            lore.add(CC.format("<yellow>Permissions: <red>%d", rank.getPermissions().size()));
            lore.add(CC.format("<yellow>Local Permissions: <red>%d", rank.getLocalPermissions().size()));
            lore.add(CC.format("<yellow>Inherited Permissions: <red>%d", rank.getInheritPermissions().size()));
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
                    .setDisplayName("<green><bold>Setup new rank")
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            player.getOpenInventory().close();
            SETUP_CHAIN.start(player);
        }
    }
}
