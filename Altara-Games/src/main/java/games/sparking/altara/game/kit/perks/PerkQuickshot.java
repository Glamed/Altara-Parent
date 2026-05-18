package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Quickshot</b>
 *
 * <p>Left-clicking with a bow fires an instant arrow at the given power without
 * drawing. Has a recharge cooldown.
 */
public class PerkQuickshot extends Perk implements Listener {

    private final String abilityName;
    private final double power;
    private final long rechargeMs;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkQuickshot(String abilityName, double power, long rechargeMs) {
        super("Quickshot", new String[]{"§eLeft-click §7bow to §a" + abilityName + "§7. Cooldown: §a" + (rechargeMs / 1000) + "s§7."});
        this.abilityName = abilityName;
        this.power = power;
        this.rechargeMs = rechargeMs;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.BOW) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < rechargeMs) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);
        Arrow arrow = player.launchProjectile(Arrow.class, player.getLocation().getDirection().multiply(power));
        arrow.setShooter(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
        player.sendMessage("§6" + abilityName + " §7fired!");
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
    }
}

