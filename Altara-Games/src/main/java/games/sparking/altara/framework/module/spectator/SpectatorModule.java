package games.sparking.altara.framework.module.spectator;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameModule;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.annotation.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Drop-in {@link GameModule} that provides a complete spectator experience for any game.
 *
 * <p>Add this to your game's {@link Game#modules()} list:
 * <pre>
 * {@literal @}Override
 * public List{@literal <GameModule>} modules() {
 *     return List.of(
 *         new SpectatorModule(),
 *         new MyCombatModule(this)
 *     );
 * }
 * </pre>
 *
 * <p>All handlers only fire when the player's state is {@link GameState#SPECTATING},
 * so active players are completely unaffected.
 *
 * <p><b>What this module provides:</b>
 * <ul>
 *   <li>Damage cancellation — spectators cannot take damage.</li>
 *   <li>Inventory protection — spectators cannot move items in containers.</li>
 *   <li>Item-drop prevention — spectators cannot drop items.</li>
 *   <li>Interaction prevention — spectators cannot use/place blocks.</li>
 *   <li>Automatic {@link GameMode#SPECTATOR} enforcement on player join (handles
 *       reconnects mid-session).</li>
 * </ul>
 *
 * <p>Your game should also call {@link org.bukkit.entity.Player#setGameMode(GameMode)}
 * with {@link GameMode#SPECTATOR} when first switching a player to spectating, so
 * that the client receives the correct UI immediately.
 */
public class SpectatorModule extends GameModule {

    // -------------------------------------------------------------------------
    // Damage
    // -------------------------------------------------------------------------

    /** Spectators are immune to all damage. */
    @GameEvent(value = EntityDamageEvent.class, states = {GameState.SPECTATING})
    public void onSpectatorDamage(EntityDamageEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Inventory
    // -------------------------------------------------------------------------

    /** Spectators cannot click inside inventories. */
    @GameEvent(value = InventoryClickEvent.class, states = {GameState.SPECTATING})
    public void onSpectatorInventoryClick(InventoryClickEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    /** Spectators cannot drop items. */
    @GameEvent(value = PlayerDropItemEvent.class, states = {GameState.SPECTATING})
    public void onSpectatorDrop(PlayerDropItemEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // World interaction
    // -------------------------------------------------------------------------

    /** Spectators cannot interact with blocks or entities. */
    @GameEvent(value = PlayerInteractEvent.class, states = {GameState.SPECTATING})
    public void onSpectatorInteract(PlayerInteractEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Reconnect handling
    // -------------------------------------------------------------------------

    /**
     * Re-applies {@link GameMode#SPECTATOR} to a player who reconnects while
     * still tracked as a spectator in this game session.
     */
    @GameEvent(value = PlayerJoinEvent.class, states = {GameState.SPECTATING})
    public void onSpectatorReconnect(PlayerJoinEvent event, Game game, Player player, GameState state) {
        player.setGameMode(GameMode.SPECTATOR);
    }
}

