package games.sparking.altara.grant.menu;


import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.chatinput.ChatInput;
import games.sparking.blazora.chatinput.ChatInputChain;
import games.sparking.blazora.grant.input.GrantDurationInput;
import games.sparking.blazora.grant.input.GrantReasonInput;
import games.sparking.blazora.menu.Button;
import games.sparking.blazora.menu.Menu;
import games.sparking.blazora.profile.Profile;
import games.sparking.blazora.utils.CC;
import games.sparking.blazora.utils.ItemBuilder;
import games.sparking.blazora.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor
public class GrantDurationMenu extends Menu {

    private static final long[] DURATION_BUTTONS = new long[]{
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.DAYS.toMillis(3),
            TimeUnit.DAYS.toMillis(7),
            TimeUnit.DAYS.toMillis(14),
            TimeUnit.DAYS.toMillis(30),
            TimeUnit.DAYS.toMillis(60),
            TimeUnit.DAYS.toMillis(90)
    };

    private static final ChatInput<String> REASON_INPUT = new GrantReasonInput(BlazoraPaper.getPaperInstance());

    private static final ChatInputChain DURATION_REASON_CHAIN = new ChatInputChain()
            .next(new GrantDurationInput(BlazoraPaper.getPaperInstance()))
            .next(new GrantReasonInput(BlazoraPaper.getPaperInstance()));


    private final BlazoraPaper zircon;
    private final Profile profile;
    private boolean clicked = false;

    @Override
    public String getTitle(Player player) {
        return "Select a duration " + profile.getGrantProcedure().getRank().getName();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (long duration : DURATION_BUTTONS)
            buttons.put(buttons.size(), new DurationButton(duration, Material.PAPER));

        buttons.put(buttons.size(), new DurationButton(-1, Material.MAP));

        int size = calculateSize(buttons) - 1;
        int slot = buttons.get(size) == null ? size : size + 9;
        buttons.put(slot, new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.BOOK)
                        .setDisplayName(CC.YELLOW + CC.BOLD + "Custom")
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                clicked = true;
                player.closeInventory();
                DURATION_REASON_CHAIN.start(player);
            }
        });
        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (!clicked) {
            profile.setGrantProcedure(null);
            player.sendMessage(CC.RED + "You cancelled the grant procedure.");
        }
    }

    @RequiredArgsConstructor
    public class DurationButton extends Button {

        private final long duration;
        private final Material material;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(material)
                    .setDisplayName(CC.YELLOW + CC.BOLD + TimeUtils.formatDetailed(duration))
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            clicked = true;
            profile.getGrantProcedure().setDuration(duration);
            player.closeInventory();
            REASON_INPUT.send(player);
        }
    }
}
