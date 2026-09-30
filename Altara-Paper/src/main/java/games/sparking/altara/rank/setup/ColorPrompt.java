package games.sparking.altara.rank.setup;

import games.sparking.altara.rank.commands.RankCommands;

public class ColorPrompt extends RankSetupPrompt<String> {

    public ColorPrompt() {
        super(String.class, "Choose a name color.", "Enter a color tag, like *<red>* or *<#ff8800>*.",
                (player, rank, input) -> RankCommands.INSTANCE.rankSetColor(player, rank, input));
    }
}
