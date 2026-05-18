package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Speed</b>
 *
 * <p>Gives the player permanent Speed at the given level (0 = Speed I, 1 = Speed II, …).
 */
public class PerkSpeed extends Perk {

    private final int level;

    public PerkSpeed(int level) {
        super("Speed", new String[]{"§7Permanent §aSpeed " + (level + 1) + "§7."});
        this.level = level;
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level, true, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
    }
}

