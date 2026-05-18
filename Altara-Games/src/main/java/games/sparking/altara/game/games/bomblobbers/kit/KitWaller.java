package games.sparking.altara.game.games.bomblobbers.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.perks.PerkWallBuilder;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkDoubleJump;
import org.bukkit.Material;

/**
 * <b>Waller Kit</b>
 * <ul>
 *   <li>{@link PerkWallBuilder} – 3 charges to place temporary sandstone walls</li>
 *   <li>{@link PerkDoubleJump} – double jump</li>
 * </ul>
 */
public class KitWaller extends Kit {

    public KitWaller(Game game) {
        super(game,
                "Waller",
                Material.SANDSTONE,
                new String[0],
                new PerkWallBuilder(),
                new PerkDoubleJump(0.65, 1.2)
        );
    }
}
