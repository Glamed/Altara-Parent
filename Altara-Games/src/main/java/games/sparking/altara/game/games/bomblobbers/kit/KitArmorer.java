package games.sparking.altara.game.games.bomblobbers.kit;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import games.sparking.altara.game.kit.perks.PerkDoubleJump;
import games.sparking.altara.game.kit.perks.PerkEquipArmour;
import games.sparking.altara.game.kit.perks.PerkPotionEffect;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Armorer Kit</b>
 * <ul>
 *   <li>{@link PerkEquipArmour} – full iron armour (overrides team leather)</li>
 *   <li>{@link PerkPotionEffect} – permanent Resistance I for blast protection</li>
 *   <li>{@link PerkDoubleJump} – double jump</li>
 * </ul>
 */
public class KitArmorer extends Kit {

    public KitArmorer(Game game) {
        super(game,
                "Armorer",
                Material.IRON_CHESTPLATE,
                new String[0],
                new PerkEquipArmour(
                        Material.IRON_HELMET,
                        Material.IRON_CHESTPLATE,
                        Material.IRON_LEGGINGS,
                        Material.IRON_BOOTS
                ),
                new PerkPotionEffect(PotionEffectType.RESISTANCE, 0, "Resistance I"),
                new PerkDoubleJump(0.65, 1.2)
        );
    }
}
