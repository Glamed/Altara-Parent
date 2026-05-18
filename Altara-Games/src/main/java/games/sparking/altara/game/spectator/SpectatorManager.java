package games.sparking.altara.game.spectator;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.visibility.VisibilityService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Manages the spectating experience for a single {@link Game} instance.
 *
 * <h2>Usage inside a game</h2>
 * <pre>{@code
 * // Eliminate a player and put them into spectator mode:
 * GamePlayer gp = getGamePlayer(player).orElseThrow();
 * gp.eliminate();
 * getSpectatorManager().enterSpectator(gp);
 *
 * // Give a player observer access without them being a participant:
 * addSpectator(player); // calls enterSpectator internally
 * }</pre>
 */
public class SpectatorManager {

    /** Display name of the compass given to spectators. */
    static final String COMPASS_NAME = "§bNext Player §7(Right-click)";

    private final Game game;

    public SpectatorManager(Game game) {
        this.game = game;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Transitions a {@link GamePlayer} into spectator mode:
     * <ol>
     *   <li>Sets {@link GameMode#SPECTATOR}.</li>
     *   <li>Clears the player's inventory.</li>
     *   <li>Places a <em>Next Player</em> compass in the centre hotbar slot.</li>
     *   <li>Points the compass (and spectator camera) at the first alive player.</li>
     *   <li>Refreshes visibility so alive players stop seeing this spectator.</li>
     * </ol>
     *
     * @param gp the game-player to spectate
     */
    public void enterSpectator(GamePlayer gp) {
        Player player = gp.getPlayer();
        if (player == null) return;

        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();

        // Give spectator compass
        player.getInventory().setItem(4, buildCompass());

        // Point camera & compass at the first alive player (if any)
        List<Player> alive = game.getAlivePlayers();
        if (!alive.isEmpty()) {
            Player first = alive.get(0);
            player.setSpectatorTarget(first);
            player.setCompassTarget(first.getLocation());
        }

        // Let the visibility system hide this spectator from alive players
        VisibilityService.update(player);
    }

    /**
     * Restores a spectating {@link GamePlayer} back to a normal state:
     * <ol>
     *   <li>Clears the spectator inventory (compass, etc.).</li>
     *   <li>Resets spectator target.</li>
     *   <li>Puts the player back into {@link GameMode#SURVIVAL}.</li>
     *   <li>Refreshes visibility so they are visible to everyone again.</li>
     * </ol>
     * <p>
     * This is called automatically when a spectating player leaves or the
     * game ends.  Games that want a custom restoration (e.g. teleport to
     * lobby spawn) should override {@link Game#onPlayerLeave(GamePlayer)}.
     *
     * @param gp the game-player to restore
     */
    public void exitSpectator(GamePlayer gp) {
        Player player = gp.getPlayer();
        if (player == null) return;

        player.setSpectatorTarget(null);
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);

        // Make them visible to everyone again
        VisibilityService.update(player);
    }

    /**
     * Cycles the spectating {@link Player} to the <em>next</em> alive player in
     * the game (wraps around).  Called when the spectator right-clicks the compass.
     *
     * @param spectator the online spectating player
     */
    public void cycleToNextPlayer(Player spectator) {
        List<Player> alive = game.getAlivePlayers();
        if (alive.isEmpty()) {
            spectator.sendMessage("§7There are no alive players to spectate.");
            return;
        }

        org.bukkit.entity.Entity current = spectator.getSpectatorTarget();
        int currentIndex = -1;
        if (current instanceof Player currentPlayer) {
            for (int i = 0; i < alive.size(); i++) {
                if (alive.get(i).getUniqueId().equals(currentPlayer.getUniqueId())) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int nextIndex = (currentIndex + 1) % alive.size();
        Player next = alive.get(nextIndex);

        spectator.setSpectatorTarget(next);
        spectator.setCompassTarget(next.getLocation());
        spectator.sendMessage("§7Spectating §b" + next.getName());
    }

    /**
     * Cycles the spectating {@link Player} to the <em>previous</em> alive player
     * in the game (wraps around).  Called when the spectator left-clicks the compass
     * or sneaks and right-clicks.
     *
     * @param spectator the online spectating player
     */
    public void cycleToPrevPlayer(Player spectator) {
        List<Player> alive = game.getAlivePlayers();
        if (alive.isEmpty()) {
            spectator.sendMessage("§7There are no alive players to spectate.");
            return;
        }

        org.bukkit.entity.Entity current = spectator.getSpectatorTarget();
        int currentIndex = 0;
        if (current instanceof Player currentPlayer) {
            for (int i = 0; i < alive.size(); i++) {
                if (alive.get(i).getUniqueId().equals(currentPlayer.getUniqueId())) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int prevIndex = (currentIndex - 1 + alive.size()) % alive.size();
        Player prev = alive.get(prevIndex);

        spectator.setSpectatorTarget(prev);
        spectator.setCompassTarget(prev.getLocation());
        spectator.sendMessage("§7Spectating §b" + prev.getName());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ItemStack buildCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(COMPASS_NAME);
            meta.setLore(Arrays.asList(
                    "§7Right-click §8→ §bnext player",
                    "§7Sneak + Right-click §8→ §bprevious player"
            ));
            compass.setItemMeta(meta);
        }
        return compass;
    }
}

