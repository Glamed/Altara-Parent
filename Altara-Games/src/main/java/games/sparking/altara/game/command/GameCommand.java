package games.sparking.altara.game.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.GameIdRef;
import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.GameState;
import games.sparking.altara.game.GameTypeRef;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Provides the {@code /game} command tree for managing game instances at runtime.
 *
 * <table>
 *   <tr><th>Sub-command</th><th>Description</th></tr>
 *   <tr><td>{@code /game types}</td><td>Lists all registered game types.</td></tr>
 *   <tr><td>{@code /game list}</td><td>Lists all currently active game instances.</td></tr>
 *   <tr><td>{@code /game create <type>}</td><td>Creates and opens a new game instance.</td></tr>
 *   <tr><td>{@code /game join <shortId>}</td><td>Joins an open game instance (player only).</td></tr>
 *   <tr><td>{@code /game leave}</td><td>Leaves your current game (player only).</td></tr>
 *   <tr><td>{@code /game start [shortId]}</td><td>Force-starts a game instance.</td></tr>
 *   <tr><td>{@code /game stop [shortId]}</td><td>Force-stops a game instance.</td></tr>
 *   <tr><td>{@code /game info [shortId]}</td><td>Displays info about a game instance.</td></tr>
 * </table>
 */
@Header(
        primaryColor   = "&3",
        secondaryColor = "&b",
        tertiaryColor  = "&b",
        header         = "Game",
        subHeader      = "Management"
)
public class GameCommand {

    // =========================================================================
    // /game types
    // =========================================================================

    @Command(
            names = "game types",
            description = "List all registered game types.",
            permission = "op"
    )
    public void onTypes(CommandSender sender) {
        GameManager gm = GameManager.getInstance();
        sender.sendMessage(CC.noticeMsg("Registered game types:"));
        if (gm.getRegisteredTypes().isEmpty()) {
            sender.sendMessage(CC.GRAY + "  (none)");
        } else {
            gm.getRegisteredTypes().forEach(id ->
                    sender.sendMessage(CC.AQUA + "  • " + CC.WHITE + id)
            );
        }
    }

    // =========================================================================
    // /game list
    // =========================================================================

    @Command(
            names = "game list",
            description = "List all active game instances.",
            permission = "op"
    )
    public void onList(CommandSender sender) {
        GameManager gm = GameManager.getInstance();
        sender.sendMessage(CC.noticeMsg("Active game instances: " + CC.WHITE + gm.getActiveGames().size()));
        if (gm.getActiveGames().isEmpty()) {
            sender.sendMessage(CC.GRAY + "  (none)");
        } else {
            gm.getActiveGames().values().forEach(g ->
                    sender.sendMessage(CC.AQUA + "  [" + g.getShortId() + "] " +
                            CC.WHITE + g.getName() + CC.GRAY + " – " +
                            stateColor(g.getState()) + g.getState().name() +
                            CC.GRAY + " (" + g.getPlayers().size() + "/" + g.getMaxPlayers() + ")")
            );
        }
    }

    // =========================================================================
    // /game create <type>
    // =========================================================================

    @Command(
            names = "game create",
            description = "Create a new game instance.",
            permission = "op"
    )
    public void onCreate(CommandSender sender, @Param(name = "type") GameTypeRef type) {
        Optional<Game> result = GameManager.getInstance().createGame(type.value());
        if (result.isEmpty()) {
            sender.sendMessage(CC.errorMsg("Unknown game type: " + CC.WHITE + type.value()));
            sender.sendMessage(CC.GRAY + "Run " + CC.AQUA + "/game types" + CC.GRAY + " to see available types.");
            return;
        }
        Game game = result.get();
        sender.sendMessage(CC.successMsg("Created game",
                CC.WHITE + game.getName() + CC.GREEN + " [" + game.getShortId() + "]"));
    }

    // =========================================================================
    // /game join <shortId>
    // =========================================================================

    @Command(
            names = "game join",
            description = "Join an open game instance.",
            playerOnly = true
    )
    public void onJoin(Player sender, @Param(name = "id") GameIdRef idRef) {
        Optional<Game> result = GameManager.getInstance().getGameByShortId(idRef.value());
        if (result.isEmpty()) {
            sender.sendMessage(CC.errorMsg("No active game found with ID: " + CC.WHITE + idRef.value()));
            return;
        }
        Game game = result.get();

        if (!game.isRecruiting()) {
            sender.sendMessage(CC.errorMsg("That game is no longer accepting players."));
            return;
        }

        boolean joined = game.addPlayer(sender);
        if (!joined) {
            sender.sendMessage(CC.errorMsg(
                    game.hasPlayer(sender)       ? "You are already in that game." :
                    game.getPlayers().size() >= game.getMaxPlayers() ? "That game is full." :
                    GameManager.getInstance().getPlayerGame(sender) != null ? "You are already in another game." :
                    "Could not join the game."
            ));
            return;
        }
        sender.sendMessage(CC.successMsg("Joined", CC.WHITE + game.getName() +
                CC.GREEN + " [" + game.getShortId() + CC.GREEN + "]."));
    }

    // =========================================================================
    // /game leave
    // =========================================================================

    @Command(
            names = "game leave",
            description = "Leave your current game.",
            playerOnly = true
    )
    public void onLeave(Player sender) {
        Game current = GameManager.getInstance().getPlayerGame(sender);
        if (current == null) {
            sender.sendMessage(CC.errorMsg("You are not in any game."));
            return;
        }
        current.removePlayer(sender);
        sender.sendMessage(CC.successMsg("Left", CC.WHITE + current.getName() + CC.GREEN + "."));
    }

    // =========================================================================
    // /game start [shortId]
    // =========================================================================

    @Command(
            names = "game start",
            description = "Force-start a game (or your current game).",
            permission = "op"
    )
    public void onStart(CommandSender sender, @Param(name = "id", defaultValue = "") GameIdRef idRef) {
        Game game = resolveGame(sender, idRef.value());
        if (game == null) return;

        if (game.isLive()) {
            sender.sendMessage(CC.errorMsg("That game is already running."));
            return;
        }
        if (game.hasEnded()) {
            sender.sendMessage(CC.errorMsg("That game has already ended."));
            return;
        }

        if (game.getState() == GameState.Recruit) game.setState(GameState.Prepare);
        if (game.getState() == GameState.Prepare)  game.setState(GameState.Live);

        sender.sendMessage(CC.successMsg("Started", CC.WHITE + game.getName() +
                CC.GREEN + " [" + game.getShortId() + CC.GREEN + "]."));
    }

    // =========================================================================
    // /game stop [shortId]
    // =========================================================================

    @Command(
            names = "game stop",
            description = "Force-stop (end) a game.",
            permission = "op"
    )
    public void onStop(CommandSender sender, @Param(name = "id", defaultValue = "") GameIdRef idRef) {
        Game game = resolveGame(sender, idRef.value());
        if (game == null) return;

        if (game.hasEnded()) {
            sender.sendMessage(CC.errorMsg("That game has already ended."));
            return;
        }

        if (game.isLive() || game.isPreparing() || game.isRecruiting()) {
            game.setState(GameState.End);
            game.destroy();
        }

        sender.sendMessage(CC.successMsg("Stopped", CC.WHITE + game.getName() +
                CC.GREEN + " [" + game.getShortId() + CC.GREEN + "]."));
    }

    // =========================================================================
    // /game info [shortId]
    // =========================================================================

    @Command(
            names = "game info",
            description = "Show information about a game instance.",
            permission = "op"
    )
    public void onInfo(CommandSender sender, @Param(name = "id", defaultValue = "") GameIdRef idRef) {
        Game game = resolveGame(sender, idRef.value());
        if (game == null) return;

        sender.sendMessage(CC.AQUA + CC.BOLD + "── Game Info ──────────────────────────");
        sender.sendMessage(CC.AQUA + " Name:    " + CC.WHITE + game.getName());
        sender.sendMessage(CC.AQUA + " ID:      " + CC.WHITE + game.getShortId());
        sender.sendMessage(CC.AQUA + " State:   " + stateColor(game.getState()) + game.getState().name());
        sender.sendMessage(CC.AQUA + " Players: " + CC.WHITE + game.getPlayers().size() + "/" + game.getMaxPlayers() +
                CC.GRAY + " (min: " + game.getMinPlayers() + ")");
        sender.sendMessage(CC.AQUA + " Alive:   " + CC.WHITE + game.getAliveCount());
        sender.sendMessage(CC.AQUA + " Teams:   " + CC.WHITE + game.getTeams().size());
        if (game.getStartTime() != -1) {
            long elapsed = System.currentTimeMillis() - game.getStartTime();
            sender.sendMessage(CC.AQUA + " Running: " + CC.WHITE + formatDuration(elapsed));
        }
        sender.sendMessage(CC.AQUA + " Desc:    " + CC.GRAY + game.getDescription());
        sender.sendMessage(CC.AQUA + CC.BOLD + "───────────────────────────────────────");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Resolves a game from a short ID string, or (if the sender is a player and the
     * string is empty) the player's current game.
     *
     * @return the resolved game, or {@code null} (with an error message sent) on failure
     */
    private Game resolveGame(CommandSender sender, String shortId) {
        if (shortId == null || shortId.isEmpty()) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.errorMsg("Provide a game ID when running from console."));
                return null;
            }
            Game current = GameManager.getInstance().getPlayerGame(player);
            if (current == null) {
                sender.sendMessage(CC.errorMsg("You are not in any game. Provide a game ID."));
                return null;
            }
            return current;
        }

        Optional<Game> result = GameManager.getInstance().getGameByShortId(shortId);
        if (result.isEmpty()) {
            sender.sendMessage(CC.errorMsg("No active game found with ID: " + CC.WHITE + shortId));
            return null;
        }
        return result.get();
    }

    /** Returns a chat colour string appropriate for the given state. */
    private String stateColor(GameState state) {
        return switch (state) {
            case Recruit  -> CC.GREEN;
            case Prepare  -> CC.YELLOW;
            case Live     -> CC.AQUA;
            case End      -> CC.RED;
            case Dead     -> CC.DGRAY;
            default       -> CC.GRAY;
        };
    }

    /** Formats a millisecond duration as {@code mm:ss}. */
    private String formatDuration(long millis) {
        long total = millis / 1000;
        long minutes = total / 60;
        long seconds = total % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
