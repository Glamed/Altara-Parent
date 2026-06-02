package games.sparking.altara.game;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Perk {

    private final String name;
    private final String description;

    private final boolean display;
    private int upgradeLevel;


}
