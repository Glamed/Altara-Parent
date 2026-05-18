package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Permanent Potion Effect</b>
 *
 * <p>Applies a potion effect with an effectively infinite duration when the kit is
 * given to a player, and removes it when they leave the game.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * new PerkPotionEffect(PotionEffectType.RESISTANCE, 0, "Resistance I")
 * // amplifier 0 = level I, 1 = level II, etc.
 * }</pre>
 */
public class PerkPotionEffect extends Perk {

    private final PotionEffectType type;
    /** Amplifier (0 = level I, 1 = level II, …). */
    private final int amplifier;

    public PerkPotionEffect(PotionEffectType type, int amplifier, String displayName) {
        super(displayName, new String[]{"§7Permanent §a" + displayName + "§7."});
        this.type = type;
        this.amplifier = amplifier;
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(type);
    }
}
