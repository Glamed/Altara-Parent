package games.sparking.altara.rank.menu;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.buttons.BackButton;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;


@RequiredArgsConstructor
public class RankEditingMenu extends Menu {

    public static final Map<UUID, UUID> RANK_SETUPS = new HashMap<>();

    private final Profile profile;
    private final Rank rank;
    private boolean save = false;

    public Component getTitle(Player player) {
        return CC.format("Editing: " + rank.getName());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(10, new SetWeightButton());
        buttons.put(11, new AddPermissionButton(false));
        buttons.put(12, new AddPermissionButton(true));
        buttons.put(13, new ToggleInheritButton());
        buttons.put(14, new SetPrefixButton());
        buttons.put(15, new SetColorButton());
        buttons.put(16, new ToggleDisguisableButton());

        buttons.put(19, new SetQueuePriorityButton());
        buttons.put(20, new RemovePermissionButton(false));
        buttons.put(21, new RemovePermissionButton(true));
        buttons.put(23, new SetSuffixButton());
        buttons.put(24, new SetChatColorButton());

        buttons.put(35, new BackButton(new RankEditOverviewMenu(profile)));
        return buttons;
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public boolean isClickUpdate() {
        return true;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.FILL;
    }

    @Override
    public void onClose(Player player) {
        if (save) {
            rank.save(player, () -> {
            });
        }
    }

    @RequiredArgsConstructor
    public class AddPermissionButton extends Button {

        private final boolean local;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(local ? Material.OAK_BUTTON : Material.STONE_BUTTON)
                    .setDisplayName(CC.format("<yellow><bold>Add %sPermission", local ? "local " : ""))
                    .setLore(CC.format(
                            "<yellow>%sPermissions: <red>%d",
                            local ? "Local " : "",
                            local ? rank.getLocalPermissions().size() : rank.getPermissions().size()
                    )).build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<String>(String.class)
                    .text("<yellow>Please enter the permission you would like to add, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the permission adding process.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        if (local) {
                            if (rank.getLocalPermissions().contains(input.toLowerCase())) {
                                player.sendMessage(CC.format("<red>Rank <yellow>%s <red>already has permission <yellow>%s<red>.",
                                        rank.getName(), input));
                                return true;
                            }
                            rank.getLocalPermissions().add(input.toLowerCase());
                        } else {
                            if (rank.getPermissions().contains(input.toLowerCase())) {
                                player.sendMessage(CC.format("<red>Rank <yellow>%s <red>already has permission <yellow>%s<red>.",
                                        rank.getName(), input));
                                return true;
                            }
                            rank.getPermissions().add(input.toLowerCase());
                        }
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You added permission <red>%s <yellow>to rank %s<yellow>.",
                                input, rank.getName()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    @RequiredArgsConstructor
    public class RemovePermissionButton extends Button {

        private final boolean local;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(local ? Material.OAK_BUTTON : Material.STONE_BUTTON)
                    .setDisplayName(CC.format("<yellow><bold>Remove %sPermission", local ? "local " : ""))
                    .setLore(CC.format(
                            "<yellow>%sPermissions: <red>%d",
                            local ? "Local " : "",
                            local ? rank.getLocalPermissions().size() : rank.getPermissions().size()
                    )).build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<String>(String.class)
                    .text("<yellow>Please enter the permission you would like to remove, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the permission removal process.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        if (local) {
                            if (!rank.getLocalPermissions().contains(input.toLowerCase())) {
                                player.sendMessage(CC.format("<red>Rank <yellow>%s <red>doesn't have permission <yellow>%s<red>.",
                                        rank.getName(), input));
                                return true;
                            }
                            rank.getLocalPermissions().remove(input.toLowerCase());
                        } else {
                            if (!rank.getPermissions().contains(input.toLowerCase())) {
                                player.sendMessage(CC.format("<red>Rank <yellow>%s <red>doesn't have permission <yellow>%s<red>.",
                                        rank.getName(), input));
                                return true;
                            }
                            rank.getPermissions().remove(input.toLowerCase());
                        }
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You removed permission <red>%s <yellow>from rank %s<yellow>.",
                                input, rank.getName()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class ToggleDisguisableButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(rank.isDisguisable() ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName("<yellow><bold>Toggle Disguisable")
                    .setLore(CC.format("<yellow>Disguisable: " + (rank.isDisguisable() ? "<green>true" : "<red>false")))
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            rank.setDisguisable(!rank.isDisguisable());
            save = true;
            player.sendMessage(CC.format("<yellow>You set the disguisable status of " + rank.getName() +
                    " to " + (rank.isDisguisable() ? "<green>true" : "<red>false") + "<yellow>."));
        }
    }

    public class SetPrefixButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.OAK_SIGN)
                    .setDisplayName("<yellow><bold>Set Prefix")
                    .setLore(CC.format("<yellow>Prefix: %sExample", rank.getPrefix()))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<String>(String.class)
                    .text("<yellow>Please enter the new prefix for this rank, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the prefix change.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        rank.setPrefix(input);
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You set the prefix of %s <yellow>to %sExample<yellow>.",
                                rank.getName(), rank.getPrefix()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class SetSuffixButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.OAK_SIGN)
                    .setDisplayName("<yellow><bold>Set Suffix")
                    .setLore(CC.format("<yellow>Suffix: <white>Example%s", rank.getSuffix()))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<String>(String.class)
                    .text("<yellow>Please enter the new suffix for this rank, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the suffix change.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        rank.setSuffix(input);
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You set the suffix of %s <yellow>to <white>Example%s<yellow>.",
                                rank.getName(), rank.getSuffix()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class SetColorButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("<yellow><bold>Set Color")
                    .setLore(CC.format("<yellow>Color: %sExample", rank.getColor()))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<String>(String.class)
                    .text("<yellow>Please enter the new color for this rank, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the color change.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        if (input.contains(" ")) {
                            player.sendMessage(CC.format("<red>The color cannot contain a white space."));
                            return false;
                        }
                        rank.setColor(input);
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You set the color of %s <yellow>to %sExample<yellow>.",
                                rank.getName(), rank.getColor()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class SetChatColorButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("<yellow><bold>Set Chat Color")
                    .setLore(CC.format("<yellow>Chat Color: %sExample", rank.getChatColor()))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<String>(String.class)
                    .text("<yellow>Please enter the new chat color for this rank, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the chat color change.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        if (input.contains(" ")) {
                            player.sendMessage(CC.format("<red>The chat color cannot contain a white space."));
                            return false;
                        }
                        rank.setChatColor(input);
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You set the chat color of %s <yellow>to %sExample<yellow>.",
                                rank.getName(), rank.getChatColor()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class ToggleInheritButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            List<Component> lore = new ArrayList<>();
            if (rank.getInherits().isEmpty()) {
                lore.add(CC.format("<yellow>Inherits: <red>None"));
            } else {
                lore.add(CC.format("<yellow>Inherits:"));
                rank.getInherits().forEach(inherit -> lore.add(
                        CC.format("<gray> - " + AltaraPaper.getSharedInstance().getRankService().getRank(inherit.getUuid()).getName())));
            }

            return new ItemBuilder(Material.BOOK)
                    .setDisplayName("<yellow><bold>Toggle Inherit")
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<Rank>(Rank.class)
                    .text("<yellow>Please enter the name of the child, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the inherit toggling.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        if (rank.getInherits().contains(input)) {
                            rank.getInherits().remove(input);
                            player.sendMessage(CC.format("<yellow>You made %s <yellow>no longer inherit %s<yellow>.",
                                    rank.getName(), input.getName()));
                        } else {
                            rank.getInherits().add(input);
                            player.sendMessage(CC.format("<yellow>You made %s <yellow>inherit %s<yellow>.",
                                    rank.getName(), input.getName()));
                        }
                        rank.save(player::sendMessage, () -> {});
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class SetWeightButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.LEVER)
                    .setDisplayName("<yellow><bold>Set Weight")
                    .setLore(CC.format("<yellow>Weight: <red>%d", rank.getWeight()))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<Integer>(Integer.class)
                    .text("<yellow>Please enter the new weight for this rank, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the weight change.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        rank.setWeight(input);
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You set the weight of %s <yellow>to <red>%d<yellow>.",
                                rank.getName(), rank.getWeight()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

    public class SetQueuePriorityButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.LEVER)
                    .setDisplayName("<yellow><bold>Set Queue Priority")
                    .setLore(CC.format("<yellow>Queue Priority: <red>%d", rank.getQueuePriority()))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.getOpenInventory().close();
            new ChatInput<Integer>(Integer.class)
                    .text("<yellow>Please enter the new queue priority for this rank, or say <red>cancel</red> to cancel.")
                    .escapeMessage("<red>You cancelled the queue priority change.")
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((player, input) -> {
                        rank.setQueuePriority(input);
                        rank.save(player::sendMessage, () -> {});
                        player.sendMessage(CC.format("<yellow>You set the queue priority of %s <yellow>to <red>%d<yellow>.",
                                rank.getName(), rank.getQueuePriority()));
                        openMenu(player);
                        return true;
                    }).send(whoClicked);
        }
    }

}
