package games.sparking.altara.rank.setup;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * One step of the {@code New Rank} chat flow.  Each step after {@link NamePrompt} applies
 * its value to the rank that prompt created.
 */
abstract class RankSetupPrompt<T> extends ChatInput<T> {

    /** @param applier applies the value; {@code null} if the subclass installs its own consumer */
    RankSetupPrompt(Class<T> type, String heading, String prompt, Applier<T> applier) {
        super(type);
        text(CC.notice(heading, prompt), CC.info("Type *cancel* to stop setting up the rank."));
        escapeMessage(CC.info("Rank setup stopped. You can finish it in */rank edit*."));
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        if (applier != null) {
            accept((player, input) -> {
                Rank rank = setupRank(player);
                if (rank == null) {
                    player.sendMessage(CC.error("Setup expired.", "The rank you were setting up no longer exists."));
                    return true;
                }
                return applier.apply(player, rank, input);
            });
        }
    }

    static Rank setupRank(Player player) {
        UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
        return rankId == null ? null : Altara.getSharedInstance().getRankService().getRank(rankId);
    }

    @FunctionalInterface
    interface Applier<T> {
        /** Apply {@code input}; return {@code false} to reject it and ask again. */
        boolean apply(Player player, Rank rank, T input);
    }
}
