package games.sparking.altara.grant.menu;

import games.sparking.altara.chatinput.ChatInputChain;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.grant.input.GrantDurationInput;
import games.sparking.altara.grant.input.GrantReasonInput;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Step 2 of {@code /grant}: pick a preset duration, permanent, or type a custom one. */
public class GrantDurationMenu extends Menu {

    private static final long[] PRESETS = {
            TimeUnit.DAYS.toMillis(1), TimeUnit.DAYS.toMillis(3), TimeUnit.DAYS.toMillis(7),
            TimeUnit.DAYS.toMillis(14), TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(60),
            TimeUnit.DAYS.toMillis(90)
    };
    private static final int[] PRESET_SLOTS = {11, 12, 13, 14, 15, 21, 22};

    private static final GrantReasonInput REASON_INPUT = new GrantReasonInput();
    private static final ChatInputChain CUSTOM_CHAIN = new ChatInputChain()
            .next(new GrantDurationInput())
            .next(new GrantReasonInput());

    private final Profile profile;
    private boolean continued = false;

    public GrantDurationMenu(Profile profile) {
        this.profile = profile;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Grant", "Duration");
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
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (int i = 0; i < PRESETS.length; i++) {
            buttons.put(PRESET_SLOTS[i], new DurationButton(PRESETS[i]));
        }
        buttons.put(23, new DurationButton(-1));
        buttons.put(31, new CustomButton());

        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());
        GrantProcedure procedure = profile.getGrantProcedure();
        if (procedure != null) {
            buttons.put(Gui.backSlot(getSize()), Gui.backButton("Rank selection", () -> {
                continued = true;
                return new GrantRankMenu(procedure);
            }));
        }
        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (!continued) {
            profile.setGrantProcedure(null);
            player.sendMessage(CC.info("Grant cancelled."));
        }
    }

    private class DurationButton extends Button {

        private final long duration;

        DurationButton(long duration) {
            this.duration = duration;
        }

        @Override
        public ItemStack getItem(Player player) {
            boolean permanent = duration == -1;
            return new ItemBuilder(permanent ? Material.NETHER_STAR : Material.CLOCK)
                    .setDisplayName(Gui.name(permanent ? "Permanent" : Time.formatDetailed(duration)))
                    .setLore(Gui.lore()
                            .text(permanent ? "The grant never expires." : "The grant expires after this time.")
                            .cta("choose this duration")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            GrantProcedure procedure = profile.getGrantProcedure();
            if (procedure == null) return;

            continued = true;
            procedure.setDuration(duration);
            player.closeInventory();
            REASON_INPUT.send(player);
        }
    }

    private class CustomButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName(Gui.name("Custom", "Duration"))
                    .setLore(Gui.lore()
                            .text("Type any duration in chat,", "like 12h or 2w3d.")
                            .cta("enter a duration")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            continued = true;
            player.closeInventory();
            CUSTOM_CHAIN.start(player);
        }
    }
}
