package games.sparking.altara.rank.setup;

import games.sparking.altara.rank.commands.RankCommands;

public class WeightPrompt extends RankSetupPrompt<Integer> {

    public WeightPrompt() {
        super(Integer.class, "Choose a weight.", "Higher weights outrank lower ones.",
                (player, rank, input) -> RankCommands.INSTANCE.rankSetWeight(player, rank, input));
    }
}
