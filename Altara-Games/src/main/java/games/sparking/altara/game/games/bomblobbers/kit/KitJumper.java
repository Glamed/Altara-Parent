package games.sparking.altara.game.games.bomblobbers.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkDoubleJump;
import games.sparking.altara.game.kit.perks.PerkNoFallDamage;
import org.bukkit.Material;

/**
 * <b>Jumper Kit</b>
 * <ul>
 *   <li>{@link PerkDoubleJump} – tap jump twice to launch upward</li>
 *   <li>{@link PerkNoFallDamage} – no fall damage</li>
 * </ul>
 */
public class KitJumper extends Kit {

    public KitJumper(Game game) {
        super(game,
                "Jumper",
                Material.FEATHER,
                new String[0],           // per-perk descriptions are shown in the kit menu
                new PerkDoubleJump(0.65, 1.2),
                new PerkNoFallDamage()
        );
    }
}
