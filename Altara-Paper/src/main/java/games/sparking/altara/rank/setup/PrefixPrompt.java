package games.sparking.altara.rank.setup;

import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.chatinput.ChatInput;
import games.sparking.blazora.rank.Rank;
import games.sparking.blazora.rank.commands.RankCommands;
import games.sparking.blazora.rank.menu.RankEditingMenu;
import games.sparking.blazora.utils.CC;
import games.sparking.blazora.utils.ChatMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PrefixPrompt extends ChatInput<String> {

    private final BlazoraPaper zircon;

    public PrefixPrompt(BlazoraPaper zircon) {
        super(String.class);
        this.zircon = zircon;

        text(CC.translate("&ePlease enter the prefix for this rank, or type &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the further rank setup.");
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
            Rank rank = rankId == null ? null : zircon.getRankService().getRank(rankId);
            if (rank == null) {
                player.sendMessage(CC.RED + "The rank you were setting up no longer exists.");
                return true;
            }

            RankCommands.INSTANCE.rankSetPrefix(player, rank, input);
            // next: weight
            return true;
        });
    }

    @Override
    public void send(Player player) {
        super.send(player);

        UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
        Rank rank = rankId == null ? null : zircon.getRankService().getRank(rankId);

        if (rank == null)
            return;

        String color = rank.getColor().replace(ChatColor.COLOR_CHAR, '&');
        String suggested = String.format("&7[%s&7] %s",
                color + rank.getName().replace('-', ' '), color);

        new ChatMessage(CC.format(
                "&e(Click to get the suggested %sExample&e)",
                CC.translate(suggested)
        )).suggestCommand(suggested).send(player);
    }
}
