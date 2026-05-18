package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Shadowmeld</b>
 *
 * <p>Hold Crouch to become invisible. Invisibility ends if you attack or an enemy
 * comes within 4 blocks.
 */
public class PerkShadowmeld extends Perk implements Listener {

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();

    public PerkShadowmeld() {
        super("Shadowmeld", new String[]{
                "§7Hold §eCrouch §7to become §ainvisible§7.",
                "§7Attacking removes invisibility."
        });
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        if (event.isSneaking()) {
            active.add(player.getUniqueId());
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false, false));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
        } else {
            active.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    @EventHandler
    public void onAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!active.contains(attacker.getUniqueId())) return;
        active.remove(attacker.getUniqueId());
        attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void remove(Player player) {
        active.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void onUnregister() { active.clear(); }
}

