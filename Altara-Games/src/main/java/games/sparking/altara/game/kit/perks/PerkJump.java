package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Jump Boost</b>
 *
 * <p>Gives the player permanent Jump Boost (0 = level I, 1 = II, …).
 */
public class PerkJump extends Perk {

    private final int level;

    public PerkJump(int level) {
        super("Jump Boost", new String[]{"§7Permanent §aJump Boost " + (level + 1) + "§7."});
        this.level = level;
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, level, true, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }
}

