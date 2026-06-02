package games.sparking.altara.games.duels.module;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.annotation.RegisterModule;
import games.sparking.altara.games.duels.DuelGame;
import games.sparking.altara.games.duels.DuelMatch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Periodically refreshes the scoreboard / tab-list display for both duelling players.
 *
 * <p>Rather than a repeating task, we piggyback on {@link PlayerMoveEvent} (which
 * fires constantly during combat) to push health updates at low cost.  Each call
 * is rate-limited to at most once per 4 ticks (200 ms) per player using a simple
 * timestamp check — no scheduler, no allocations.
 */
@RegisterModule(game = "duels")
public class DuelScoreboardModule {

    /** Last update timestamp per player UUID hashCode (cheap long array). */
    private final long[] lastUpdate = new long[4096];

    private static final long UPDATE_INTERVAL_MS = 200L; // 4 ticks

    @GameEvent(value = PlayerMoveEvent.class, states = {GameState.PLAYING})
    public void onMove(PlayerMoveEvent event, Game game, Player player, GameState state) {
        // Only update if the player actually moved a block (not just head rotation)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        long now = System.currentTimeMillis();
        int slot = (player.hashCode() & 0x7FFF) % lastUpdate.length;

        if (now - lastUpdate[slot] < UPDATE_INTERVAL_MS) return;
        lastUpdate[slot] = now;

        DuelGame duelGame = (DuelGame) game;
        DuelMatch match = duelGame.getMatch(player.getUniqueId());
        if (match == null) return;

        Player opponent = match.getOpponentOnline(player.getUniqueId());
        if (opponent == null) return;

        // Show the opponent's health below their nametag using display name
        double opponentHearts = opponent.getHealth() / 2.0;
        double myHearts = player.getHealth() / 2.0;

        opponent.displayName(Component.text(opponent.getName())
                .color(NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text(String.format("%.1f❤", opponentHearts), NamedTextColor.RED)));

        player.displayName(Component.text(player.getName())
                .color(NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text(String.format("%.1f❤", myHearts), NamedTextColor.RED)));

        // Show a mini scoreboard via the player list header
        String header = "§6§lDUEL §r§7vs §e" + opponent.getName() + "\n"
                + "§aYour HP: §c" + String.format("%.1f❤", myHearts)
                + "  §7| §c" + opponent.getName() + ": §c" + String.format("%.1f❤", opponentHearts) + "\n"
                + "§7Combo: §e" + match.getCombo(player.getUniqueId());

        player.sendPlayerListHeaderAndFooter(
                Component.text(header),
                Component.text("§7Kit: §a" + match.getKit().getDisplayName()
                        + "  §7Arena: §a" + match.getArena().getName())
        );
    }
}


