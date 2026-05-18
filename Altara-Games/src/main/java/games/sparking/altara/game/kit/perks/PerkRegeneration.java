package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * <b>Regeneration</b>
 *
 * <p>Gives the player permanent Regeneration (0 = Regen I, 1 = Regen II, …).
 */
public class PerkRegeneration extends Perk {

    private final int level;

    public PerkRegeneration(int level) {
        super("Regeneration", new String[]{"§7Permanent §aRegeneration " + (level + 1) + "§7."});
        this.level = level;
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, level, true, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }
}

