package games.sparking.altara.rank.setup;

import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public class PrefixPrompt extends RankSetupPrompt<String> {

    public PrefixPrompt() {
        super(String.class, "Choose a prefix.", "This appears before the player's name in chat.",
                (player, rank, input) -> {
                    RankCommands.INSTANCE.rankSetPrefix(player, rank, input);
                    return true;
                });
    }

    @Override
    public void send(Player player) {
        super.send(player);

        Rank rank = setupRank(player);
        if (rank == null) return;

        String suggested = "<dark_gray>[" + rank.getColor() + rank.getName() + "<dark_gray>] ";
        player.sendMessage(CC.info("Need an example? ").append(CC.action("Click to use a suggested prefix.",
                ClickEvent.suggestCommand(suggested))));
    }
}
