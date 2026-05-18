package games.sparking.altara.game.spectator;

import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Global event listener that enforces spectator behaviour for any player who has
 * been put into spectator mode by the game framework.
 *
 * <h2>Handled events</h2>
 * <ul>
 *   <li><b>Compass interact</b> – right-click cycles to the next alive player;
 *       sneaking + right-click cycles to the previous alive player.</li>
 *   <li><b>Entity damage</b> – spectators in a game cannot be hurt.</li>
 *   <li><b>Item pickup</b> – spectators cannot pick up items.</li>
 *   <li><b>Item drop</b> – spectators cannot drop items.</li>
 * </ul>
 *
 * <p>Register this listener once at plugin startup via
 * {@link org.bukkit.plugin.PluginManager#registerEvents(Listener, org.bukkit.plugin.Plugin)}.
 */
public class SpectatorListener implements Listener {

    // =========================================================================
    // Compass – cycle through alive players
    // =========================================================================

    /**
     * Handles right-clicking the spectator compass to cycle through alive players.
     * <ul>
     *   <li>Normal right-click → next alive player.</li>
     *   <li>Sneaking + right-click → previous alive player.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCompassInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!isSpectatorCompass(held)) {
            // Also check off-hand
            held = player.getInventory().getItemInOffHand();
            if (!isSpectatorCompass(held)) return;
        }

        Game game = GameManager.getInstance().getPlayerGame(player);
        if (game == null) return;

        GamePlayer gp = game.getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isSpectating()) return;

        event.setCancelled(true);

        if (player.isSneaking()) {
            game.getSpectatorManager().cycleToPrevPlayer(player);
        } else {
            game.getSpectatorManager().cycleToNextPlayer(player);
        }
    }

    // =========================================================================
    // Safety nets (GameMode.SPECTATOR already handles most of these, but we
    // add explicit guards for robustness and custom game-mode edge cases)
    // =========================================================================

    /** Prevents spectators in a game from taking any damage. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isGameSpectator(player)) return;
        event.setCancelled(true);
    }

    /** Prevents spectators from picking up dropped items. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpectatorPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isGameSpectator(player)) return;
        event.setCancelled(true);
    }

    /** Prevents spectators from dropping their compass or any other item. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpectatorDrop(PlayerDropItemEvent event) {
        if (!isGameSpectator(event.getPlayer())) return;
        event.setCancelled(true);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns {@code true} if the given player is currently spectating in a game
     * (i.e. they are in a game <em>and</em> their {@link GamePlayer} state is
     * {@link games.sparking.altara.game.player.GamePlayerState#SPECTATING} or
     * {@link games.sparking.altara.game.player.GamePlayerState#ELIMINATED}).
     */
    private boolean isGameSpectator(Player player) {
        Game game = GameManager.getInstance().getPlayerGame(player);
        if (game == null) return false;
        GamePlayer gp = game.getGamePlayer(player).orElse(null);
        return gp != null && gp.isSpectating();
    }

    /** Returns {@code true} if the item is the spectator compass handed out by {@link SpectatorManager}. */
    private boolean isSpectatorCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && SpectatorManager.COMPASS_NAME.equals(meta.getDisplayName());
    }
}

