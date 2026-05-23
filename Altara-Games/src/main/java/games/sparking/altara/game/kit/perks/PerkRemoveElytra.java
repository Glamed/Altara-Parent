package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Stunner</b>
 *
 * <p>When you deal damage to a gliding player, their elytra is removed for
 * {@value DISABLE_MS}ms (1 second), causing them to fall.
 */
public class PerkRemoveElytra extends Perk implements Listener {

    private static final long DISABLE_MS = 1_000;

    /** Tracks which players have had elytra removed and until when. */
    private final Map<UUID, Long> disabled = new ConcurrentHashMap<>();

    public PerkRemoveElytra() {
        super("Stunner", new String[]{
                "§7Dealing damage to gliding enemies §adisables their elytra §7for §a1s§7."
        });
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!hasPerk(attacker)) return;
        if (!getGame().hasPlayer(attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (!target.isGliding()) return;
        if (!getGame().hasPlayer(target)) return;
        var tgp = getGame().getGamePlayer(target).orElse(null);
        if (tgp == null || !tgp.isAlive()) return;

        disableElytra(target);
    }

    private void disableElytra(Player target) {
        disabled.put(target.getUniqueId(), System.currentTimeMillis() + DISABLE_MS);

        // Remove elytra from chestplate slot
        ItemStack chest = target.getInventory().getChestplate();
        if (chest != null && chest.getType() == Material.ELYTRA) {
            target.getInventory().setChestplate(null);
            target.sendMessage("§cYour elytra was disabled!");

            // Restore after delay
            long ticks = DISABLE_MS / 50;
            Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
                Long until = disabled.get(target.getUniqueId());
                if (until != null && System.currentTimeMillis() >= until) {
                    disabled.remove(target.getUniqueId());
                    if (target.isOnline()) {
                        target.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
                        target.sendMessage("§aYour elytra has been restored.");
                    }
                }
            }, ticks + 1);
        }
    }

    @Override
    public void onUnregister() {
        disabled.clear();
    }
}

