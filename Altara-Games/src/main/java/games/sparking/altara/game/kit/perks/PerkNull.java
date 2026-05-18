package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;

/** A no-op perk. Does absolutely nothing. */
public class PerkNull extends Perk {

    public PerkNull() {
        super("Null", new String[]{"§7Absolutely nothing!"});
    }
}

