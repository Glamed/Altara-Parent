package games.sparking.altara.game.games.bomblobbers.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkVelocitySelector;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkDoubleJump;
import org.bukkit.Material;

/**
 * <b>Pitcher Kit</b>
 * <ul>
 *   <li>{@link PerkVelocitySelector} – adjustable throw power via a lever item</li>
 *   <li>{@link PerkDoubleJump} – double jump</li>
 * </ul>
 */
public class KitPitcher extends Kit {

    public KitPitcher(Game game) {
        super(game,
                "Pitcher",
                Material.LEVER,
                new String[0],
                new PerkVelocitySelector(),
                new PerkDoubleJump(0.65, 1.2)
        );
    }
}
