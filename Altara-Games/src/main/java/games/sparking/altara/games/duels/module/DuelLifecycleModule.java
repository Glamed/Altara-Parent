package games.sparking.altara.games.duels.module;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.annotation.RegisterModule;
import games.sparking.altara.games.duels.DuelGame;
import games.sparking.altara.games.duels.DuelMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Protects match integrity by preventing item loss, hunger drain, and
 * inventory modifications during an active duel.
 *
 * <p>Also handles the edge case of a player disconnecting mid-match — the
 * remaining player is awarded the win automatically.
 */
@RegisterModule(game = "duels")
public class DuelLifecycleModule {

    // ── Item protection ──────────────────────────────────────────────────────

    @GameEvent(value = PlayerDropItemEvent.class, states = {GameState.PLAYING})
    public void onItemDrop(PlayerDropItemEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    @GameEvent(value = InventoryClickEvent.class, states = {GameState.PLAYING})
    public void onInventoryClick(InventoryClickEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    @GameEvent(value = InventoryDragEvent.class, states = {GameState.PLAYING})
    public void onInventoryDrag(InventoryDragEvent event, Game game, Player player, GameState state) {
        event.setCancelled(true);
    }

    // ── Hunger ───────────────────────────────────────────────────────────────

    @GameEvent(value = FoodLevelChangeEvent.class, states = {GameState.PLAYING})
    public void onHunger(FoodLevelChangeEvent event, Game game, Player player, GameState state) {
        // Freeze food level at 20 so players can sprint freely
        event.setCancelled(true);
    }

    // ── Disconnect forfeit ───────────────────────────────────────────────────

    /**
     * If a player disconnects during a match, their opponent wins automatically.
     *
     * <p>Note: {@link PlayerQuitEvent} extends {@link org.bukkit.event.player.PlayerEvent}
     * so the framework's default {@link games.sparking.altara.framework.PlayerExtractor} handles it.
     */
    @GameEvent(value = PlayerQuitEvent.class, states = {GameState.PLAYING})
    public void onQuit(PlayerQuitEvent event, Game game, Player player, GameState state) {
        DuelGame duelGame = (DuelGame) game;
        DuelMatch match = duelGame.getMatch(player.getUniqueId());
        if (match == null) return;

        // Give the win to the opponent
        duelGame.endMatch(match, match.getOpponent(player.getUniqueId()),
                "§e" + player.getName() + " §cdisconnected — their opponent wins!");
    }
}

