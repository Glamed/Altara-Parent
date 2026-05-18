package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Digger</b>
 *
 * <p>Gives permanent Haste II for fast mining.
 */
public class PerkDigger extends Perk {

    public PerkDigger() {
        super("Digger", new String[]{"§7Permanent §aHaste II§7."});
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 1, true, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.HASTE);
    }
}

