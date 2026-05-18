package games.sparking.altara.game.games.skywars;

import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import games.sparking.altara.world.AltaraWorld;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>SkyWars — Solo</h1>
 *
 * <p>Free-for-all SkyWars session. Supports multiple concurrent sessions on the
 * same server; each instance is fully isolated via the {@link BaseSkyWars} event
 * guards.
 */
public class SkyWars extends BaseSkyWars {

    // =========================================================================
    // Metadata
    // =========================================================================

    @Override public String getDescription() { return "Free-for-all sky battle — last one standing wins!"; }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onRecruit() {
        broadcast(ChatColor.AQUA + "SkyWars §7— choose your kit!");
        broadcast(ChatColor.GRAY + "Last player alive wins!");
        super.onRecruit();
    }

    @Override
    protected void onStart() {
        AltaraWorld arena = getArenaWorld();
        List<Location> spawns = new ArrayList<>();
        if (arena != null) {
            // Try explicit "Players" key first, then fall back to every spawn in the map.
            // Mineplex maps label spawns by colour (Green, Red, Blue…) rather than "Players",
            // so we collect all team-spawn lists when the dedicated key is absent.
            List<Location> playerSpawns = arena.getSpawns("Players");
            if (!playerSpawns.isEmpty()) {
                spawns.addAll(playerSpawns);
            } else {
                arena.getAllSpawns().values().forEach(spawns::addAll);
            }
        }
        startGame(spawns);
    }

    // =========================================================================
    // Solo win condition
    // =========================================================================

    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        Player p = gp.getPlayer();
        String name = (p != null) ? p.getName() : gp.getName();
        broadcast(ChatColor.GRAY + name + " was eliminated! §e(" + getAliveCount() + " left)");
        checkWinCondition();
    }

    /** Safety-net once per second. */
    @EventHandler
    public void onUpdateSec(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC || !isLive()) return;
        checkWinCondition();
    }

    private void checkWinCondition() {
        if (!isLive()) return;

        List<GamePlayer> alive = getPlayers().values().stream()
                .filter(GamePlayer::isAlive)
                .toList();

        if (alive.size() <= 1) {
            onWinnerDecided(alive.isEmpty() ? null : alive.getFirst());
            endGame();
        }
    }

    private void onWinnerDecided(@Nullable GamePlayer winner) {
        if (winner == null) {
            broadcast(ChatColor.GRAY + "No winner — everyone perished!");
        } else {
            Player p = winner.getPlayer();
            String name = (p != null) ? p.getName() : winner.getName();
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + name
                    + ChatColor.RESET + ChatColor.GOLD + " wins SkyWars!");
        }
    }
}
