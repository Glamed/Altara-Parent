package games.sparking.altara.grant.menu;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.chatinput.ChatInputChain;
import games.sparking.altara.grant.input.GrantDurationInput;
import games.sparking.altara.grant.input.GrantReasonInput;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
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

    private static final ChatInput<String> REASON_INPUT = new GrantReasonInput();

    private static final ChatInputChain DURATION_REASON_CHAIN = new ChatInputChain()
            .next(new GrantDurationInput())
            .next(new GrantReasonInput());


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
                        .setDisplayName(Component.text("Custom", CC.YELLOW, TextDecoration.BOLD))
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
            player.sendMessage(Component.text("You cancelled the grant procedure.", CC.RED));
        }
    }

    @RequiredArgsConstructor
    public class DurationButton extends Button {

        private final long duration;
        private final Material material;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(material)
                    .setDisplayName(Component.text(Time.formatDetailed(duration), CC.YELLOW, TextDecoration.BOLD))
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
