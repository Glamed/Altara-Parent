package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * <b>Vampire</b>
 *
 * <p>Heals the player for {@code recover} half-hearts when they kill an enemy player.
 */
public class PerkVampire extends Perk implements Listener {

    private final double recover;

    public PerkVampire(double recover) {
        super("Vampire", new String[]{"§7Heal §a" + recover + " §7hearts on kill."});
        this.recover = recover;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!hasPerk(killer)) return;
        double newHp = Math.min(killer.getMaxHealth(), killer.getHealth() + recover);
        killer.setHealth(newHp);
        killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
    }
}

