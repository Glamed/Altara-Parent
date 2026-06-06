package games.sparking.altara.punishment.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.RequiredArgsConstructor;
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

/**
 * Infraction-type selection menu — the first step of the punishment builder.
 *
 * <p>Opens to a grid of {@link InfractionType} options. Clicking one forwards
 * the staff member to {@link PunishActionMenu} where they can review / tweak the
 * recommended restriction actions before confirming.
 */
public class PunishMenu extends Menu {

    private final OfflinePlayer target;
    private final String        interceptedMessage;

    public PunishMenu(OfflinePlayer target) {
        this(target, null);
    }

    public PunishMenu(OfflinePlayer target, String interceptedMessage) {
        this.target             = target;
        this.interceptedMessage = interceptedMessage;
    }

    @Override
    public String getTitle(Player player) {
        return "Punish " + target.getName();
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4, new HeadButton(target));

        InfractionType[] types = InfractionType.visibleValues();
        int total = types.length;
        int index = 19;

        for (int i = 0; i < total; ) {
            int itemsThisRow  = Math.min(7, total - i);
            int rowStart      = (index / 7) * 9;
            int startOffset   = (9 - itemsThisRow) / 2;

            for (int j = 0; j < itemsThisRow; j++, i++) {
                int slot = rowStart + startOffset + j;
                buttons.put(slot, new TypeButton(types[i]));
            }
            index += 7;
        }
        return buttons;
    }

    // ── Inner buttons ──────────────────────────────────────────────────────────

    @RequiredArgsConstructor
    public class HeadButton extends Button {

        private final OfflinePlayer p;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(p.getName())
                    .setDisplayName("<gray>Punish <light_purple>" + p.getName())
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
            // Word-wrap the description to ~40 chars per line
            List<String> lore = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : infractionType.getDescription().split(" ")) {
                if (line.length() + word.length() + 1 > 40) {
                    lore.add("<gray><italic>" + line);
                    line = new StringBuilder(word);
                } else {
                    if (!line.isEmpty()) line.append(" ");
                    line.append(word);
                }
            }
            if (!line.isEmpty()) lore.add("<gray><italic>" + line);

            Material mat = infractionType.getMaterial();

            return new ItemBuilder(mat)
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName("<dark_purple><bold>" + infractionType.getDisplayName())
                    .setLore(CC.format(lore))
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget == null) {
                whoClicked.sendMessage(CC.format("<red>That player is no longer online."));
                return;
            }
            new PunishActionMenu()
                    .initialize(onlineTarget, infractionType, interceptedMessage, PunishMenu.this)
                    .openMenu(whoClicked);
        }
    }

    // (material is now part of InfractionType — see InfractionType.getMaterial())
}

