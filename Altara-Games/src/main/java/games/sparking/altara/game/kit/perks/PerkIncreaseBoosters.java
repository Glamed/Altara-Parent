package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import games.sparking.altara.game.games.skyfall.event.PlayerBoostRingEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * <b>Ring Boost</b>
 *
 * <p>When the player passes through a booster ring, their boost is multiplied by {@value MULTIPLIER}.
 */
public class PerkIncreaseBoosters extends Perk implements Listener {

    private static final double MULTIPLIER = 1.1;

    public PerkIncreaseBoosters() {
        super("Ring Boost", new String[]{
                "§7Boost rings give §a+10% §7extra boost momentum."
        });
    }

    @EventHandler
    public void onBoostRing(PlayerBoostRingEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        event.multiplyStrength(MULTIPLIER);
    }
}

