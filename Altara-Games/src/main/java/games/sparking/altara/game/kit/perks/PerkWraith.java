package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Wraith</b>
 *
 * <p>Sneak to become a ghostly wraith — briefly invisible, with increased speed and
 * the ability to pass through players (no collision). Ends early if you attack.
 */
public class PerkWraith extends Perk implements Listener {

    private static final long COOLDOWN_MS = 16000L;
    private static final int DURATION_TICKS = 60; // 3 seconds

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeTaskIds = new ConcurrentHashMap<>();

    public PerkWraith() {
        super("Wraith", new String[]{
                "§7Sneak to become a §7§lWraith§r§7 — invisible and swift.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (activeTaskIds.containsKey(player.getUniqueId())) return; // already active

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        activateWraith(player);
    }

    private void activateWraith(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATION_TICKS, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURATION_TICKS, 1, false, false));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);

        int taskId = AltaraPaper.getPaperInstance().getServer().getScheduler()
                .scheduleSyncDelayedTask(AltaraPaper.getPaperInstance(), () -> {
                    activeTaskIds.remove(player.getUniqueId());
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0),
                            15, 0.3, 0.5, 0.3, 0.02);
                }, DURATION_TICKS);

        activeTaskIds.put(player.getUniqueId(), taskId);
    }

    @Override
    public void remove(Player player) {
        Integer taskId = activeTaskIds.remove(player.getUniqueId());
        if (taskId != null) {
            AltaraPaper.getPaperInstance().getServer().getScheduler().cancelTask(taskId);
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    @Override
    public void onUnregister() {
        for (int taskId : activeTaskIds.values()) {
            AltaraPaper.getPaperInstance().getServer().getScheduler().cancelTask(taskId);
        }
        activeTaskIds.clear();
        cooldowns.clear();
    }
}

