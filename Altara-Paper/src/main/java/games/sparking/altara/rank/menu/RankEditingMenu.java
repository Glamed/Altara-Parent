package games.sparking.altara.rank.menu;

import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Edits a single rank.  Text values are entered in chat; the menu reopens afterwards. */
public class RankEditingMenu extends Menu {

    /** Rank being configured by each player in the {@code Setup new rank} chat flow. */
    public static final Map<UUID, UUID> RANK_SETUPS = new HashMap<>();

    private final Profile profile;
    private final Rank rank;

    public RankEditingMenu(Profile profile, Rank rank) {
        this.profile = profile;
        this.rank = rank;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Ranks", rank.getName());
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public boolean isClickUpdate() {
        return true;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        buttons.put(4, Button.createPlaceholder(new ItemBuilder(rank.getMaterial())
                .setDisplayName(CC.format(rank.getDisplayName()))
                .setLore(RankEditOverviewMenu.summary(rank).build())
                .build()));

        buttons.put(11, new PromptButton<>(String.class, Material.RED_DYE, "Name Color",
                () -> CC.format(rank.getColor() + "Example"),
                "Enter a color tag, like <red> or <#ff8800>.",
                (player1, input) -> RankCommands.INSTANCE.rankSetColor(player1, rank, input)));
        buttons.put(12, new PromptButton<>(String.class, Material.WHITE_DYE, "Chat Color",
                () -> CC.format(rank.getChatColor() + "Example"),
                "Enter a color tag, like <white> or <gray>.",
                (player1, input) -> RankCommands.INSTANCE.rankSetChatColor(player1, rank, input)));
        buttons.put(13, new PromptButton<>(String.class, Material.NAME_TAG, "Prefix",
                () -> CC.format(rank.getPrefix() + rank.getColor() + "Example"),
                "Enter the prefix, e.g. <dark_gray>[<red>Admin<dark_gray>] ",
                (player1, input) -> RankCommands.INSTANCE.rankSetPrefix(player1, rank, input)));
        buttons.put(14, new PromptButton<>(String.class, Material.NAME_TAG, "Suffix",
                () -> CC.format(rank.getColor() + "Example" + rank.getSuffix()),
                "Enter the suffix (use <white> for none).",
                (player1, input) -> RankCommands.INSTANCE.rankSetSuffix(player1, rank, input)));
        buttons.put(15, new DisguisableButton());

        buttons.put(20, new PromptButton<>(Integer.class, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "Weight",
                () -> Component.text(rank.getWeight()),
                "Enter the new weight (higher ranks outrank lower ones).",
                (player1, input) -> RankCommands.INSTANCE.rankSetWeight(player1, rank, input)));
        buttons.put(21, new PromptButton<>(Integer.class, Material.HOPPER, "Queue Priority",
                () -> Component.text(rank.getQueuePriority()),
                "Enter the new queue priority.",
                (player1, input) -> RankCommands.INSTANCE.rankSetQueuePriority(player1, rank, input)));
        buttons.put(22, new PromptButton<>(Rank.class, Material.BOOK, "Inherits",
                () -> Component.text(rank.getInherits().isEmpty() ? "None"
                        : String.join(", ", rank.getInherits().stream().map(Rank::getName).toList())),
                "Enter a rank to toggle inheriting it.",
                (player1, input) -> RankCommands.toggleInherit(player1, rank, input)));
        buttons.put(23, new PermissionButton(false));
        buttons.put(24, new PermissionButton(true));

        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());
        buttons.put(Gui.backSlot(getSize()), Gui.backButton("Rank editor", () -> new RankEditOverviewMenu(profile)));
        return buttons;
    }

    /** Asks for a value in chat, applies it, and reopens this menu. */
    private class PromptButton<T> extends Button {

        private final Class<T> type;
        private final Material material;
        private final String name;
        private final Supplier<Component> current;
        private final String prompt;
        private final BiConsumer<Player, T> apply;

        PromptButton(Class<T> type, Material material, String name, Supplier<Component> current,
                     String prompt, BiConsumer<Player, T> apply) {
            this.type = type;
            this.material = material;
            this.name = name;
            this.current = current;
            this.prompt = prompt;
            this.apply = apply;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(material)
                    .setDisplayName(Gui.name(name))
                    .setLore(Gui.lore()
                            .value("Current", current.get())
                            .cta("change this")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            player.closeInventory();
            new ChatInput<T>(type)
                    .text(CC.notice(name + ".", prompt), CC.info("Type *cancel* to go back."))
                    .escapeMessage(CC.info("No changes made."))
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((p, input) -> {
                        apply.accept(p, input);
                        openMenu(p);
                        return true;
                    })
                    .send(player);
        }
    }

    /** Left click adds, right click removes. */
    private class PermissionButton extends Button {

        private final boolean local;

        PermissionButton(boolean local) {
            this.local = local;
        }

        @Override
        public ItemStack getItem(Player player) {
            int count = local ? rank.getLocalPermissions().size() : rank.getPermissions().size();
            return new ItemBuilder(local ? Material.REPEATING_COMMAND_BLOCK : Material.COMMAND_BLOCK)
                    .setDisplayName(local ? Gui.name("Local", "Permissions") : Gui.name("Permissions"))
                    .setLore(Gui.lore()
                            .text(local ? "Only apply on this realm." : "Apply on every realm.")
                            .value("Count", String.valueOf(count))
                            .blank()
                            .line(Gui.cta("add a permission").append(Component.text(" (left)", Theme.TEXT)))
                            .line(Gui.cta("remove a permission").append(Component.text(" (right)", Theme.TEXT)))
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            boolean remove = clickType.isRightClick();
            Function<String, Boolean> action = remove
                    ? node -> RankCommands.removePermission(player, rank, node, local)
                    : node -> RankCommands.addPermission(player, rank, node, local);

            player.closeInventory();
            new ChatInput<String>(String.class)
                    .text(CC.notice(remove ? "Remove a permission." : "Add a permission.", "Enter the permission node."),
                            CC.info("Type *cancel* to go back."))
                    .escapeMessage(CC.info("No changes made."))
                    .onCancel(RankEditingMenu.this::openMenu)
                    .accept((p, input) -> {
                        action.apply(input);
                        openMenu(p);
                        return true;
                    })
                    .send(player);
        }
    }

    private class DisguisableButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Gui.settingMaterial(rank.isDisguisable()))
                    .setDisplayName(Gui.settingName("Disguisable", rank.isDisguisable()))
                    .setLore(Gui.lore()
                            .text("Whether staff can disguise", "as this rank.")
                            .cta(rank.isDisguisable() ? "disable" : "enable")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            RankCommands.INSTANCE.rankSetDisguisable(player, rank, !rank.isDisguisable());
        }
    }
}
