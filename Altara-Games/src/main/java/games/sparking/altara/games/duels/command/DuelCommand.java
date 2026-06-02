package games.sparking.altara.games.duels.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.games.duels.DuelArena;
import games.sparking.altara.games.duels.DuelGame;
import games.sparking.altara.games.duels.DuelKit;
import games.sparking.altara.games.duels.DuelMatch;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * All /duel sub-commands.
 *
 * <p>Registered with {@link games.sparking.altara.command.CommandService} during plugin enable.
 *
 * <pre>
 * /duel &lt;player&gt; [kit]       — challenge a player
 * /duel accept [kit]         — accept the incoming request
 * /duel deny                 — deny the incoming request
 * /duel forfeit              — surrender your current match
 * /duel stats                — view your current match status
 * /duel arena create &lt;name&gt;  — create a new arena
 * /duel arena setspawn &lt;name&gt; &lt;1|2&gt; — set a spawn point
 * /duel arena list           — list all arenas
 * /duel kits                 — list available kits
 * </pre>
 */
@Header(header = "Duel", primaryColor = "&6", secondaryColor = "&e", tertiaryColor = "&7")
public class DuelCommand {

    // ── Challenge ─────────────────────────────────────────────────────────────

    @Command(
            names = {"duel"},
            description = "Challenge a player to a duel",
            playerOnly = true
    )
    public boolean duel(Player sender,
                        @Param(name = "player") Player target,
                        @Param(name = "kit", defaultValue = "classic") String kitId) {

        if (sender.equals(target)) {
            sender.sendMessage("§cYou cannot duel yourself.");
            return false;
        }

        DuelGame game = getDuelGame();
        Optional<DuelKit> kit = game.getKit(kitId);
        if (kit.isEmpty()) {
            sender.sendMessage("§cUnknown kit: §e" + kitId
                    + "§c. Use §e/duel kits §cto see available kits.");
            return false;
        }

        game.sendRequest(sender, target, kit.get());
        return true;
    }

    // ── Accept ────────────────────────────────────────────────────────────────

    @Command(
            names = {"duel accept"},
            description = "Accept an incoming duel request",
            playerOnly = true
    )
    public boolean accept(Player sender, @Param(name = "kit", defaultValue = "classic") String kitId) {
        DuelGame game = getDuelGame();

        if (!game.hasPendingRequest(sender.getUniqueId())) {
            sender.sendMessage("§cYou have no pending duel requests.");
            return false;
        }

        Optional<DuelKit> kit = game.getKit(kitId);
        if (kit.isEmpty()) {
            sender.sendMessage("§cUnknown kit: §e" + kitId);
            return false;
        }

        return game.acceptRequest(sender, kit.get());
    }

    // ── Deny ──────────────────────────────────────────────────────────────────

    @Command(
            names = {"duel deny"},
            description = "Deny an incoming duel request",
            playerOnly = true
    )
    public boolean deny(Player sender) {
        return getDuelGame().denyRequest(sender);
    }

    // ── Forfeit ───────────────────────────────────────────────────────────────

    @Command(
            names = {"duel forfeit", "duel ff"},
            description = "Forfeit your current duel",
            playerOnly = true
    )
    public boolean forfeit(Player sender) {
        DuelGame game = getDuelGame();
        DuelMatch match = game.getMatch(sender.getUniqueId());

        if (match == null) {
            sender.sendMessage("§cYou are not in a duel.");
            return false;
        }

        game.endMatch(match, match.getOpponent(sender.getUniqueId()),
                "§e" + sender.getName() + " §cforfeited the match.");
        return true;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Command(
            names = {"duel stats", "duel info"},
            description = "View your current duel status",
            playerOnly = true
    )
    public boolean stats(Player sender) {
        DuelGame game = getDuelGame();
        DuelMatch match = game.getMatch(sender.getUniqueId());

        if (match == null) {
            sender.sendMessage("§cYou are not in a duel.");
            sender.sendMessage("§7Active matches: §e" + game.getActiveMatchCount());
            return false;
        }

        sender.sendMessage("§6§lDUEL INFO");
        sender.sendMessage("§7Kit: §a" + match.getKit().getDisplayName());
        sender.sendMessage("§7Arena: §a" + match.getArena().getName());
        sender.sendMessage("§7Duration: §e" + match.getDurationSeconds() + "s");
        sender.sendMessage("§7Your combo: §e" + match.getCombo(sender.getUniqueId()));
        return true;
    }

    // ── Kits ──────────────────────────────────────────────────────────────────

    @Command(
            names = {"duel kits"},
            description = "List all available duel kits"
    )
    public boolean kits(CommandSender sender) {
        DuelGame game = getDuelGame();
        sender.sendMessage("§6§lAvailable Kits:");
        game.getKits().forEach(k ->
                sender.sendMessage("§7 • §e" + k.getId() + " §7— §a" + k.getDisplayName()));
        return true;
    }

    // ── Arena management ──────────────────────────────────────────────────────

    @Command(
            names = {"duel arena create"},
            description = "Create a new duel arena",
            playerOnly = true,
            permission = "duels.admin"
    )
    public boolean arenaCreate(Player sender, @Param(name = "name") String name) {
        DuelGame game = getDuelGame();

        if (game.getArena(name).isPresent()) {
            sender.sendMessage("§cAn arena named §e" + name + " §calready exists.");
            return false;
        }

        DuelArena arena = new DuelArena(name);
        game.addArena(arena);
        sender.sendMessage("§aArena §e" + name + " §acreated. Now set both spawns with §e/duel arena setspawn "
                + name + " 1§a and §e/duel arena setspawn " + name + " 2§a.");
        return true;
    }

    @Command(
            names = {"duel arena setspawn"},
            description = "Set a spawn point for a duel arena",
            playerOnly = true,
            permission = "duels.admin"
    )
    public boolean arenaSetSpawn(Player sender,
                                  @Param(name = "arena") String arenaName,
                                  @Param(name = "point") int point) {

        if (point != 1 && point != 2) {
            sender.sendMessage("§cSpawn point must be §e1 §cor §e2§c.");
            return false;
        }

        DuelGame game = getDuelGame();
        Optional<DuelArena> opt = game.getArena(arenaName);

        if (opt.isEmpty()) {
            sender.sendMessage("§cArena §e" + arenaName + " §cdoes not exist. Create it first with §e/duel arena create " + arenaName + "§c.");
            return false;
        }

        DuelArena arena = opt.get();
        if (point == 1) arena.setSpawn1(sender.getLocation());
        else arena.setSpawn2(sender.getLocation());

        sender.sendMessage("§aSpawn §e" + point + " §afor arena §e" + arenaName + " §aset to your location.");

        if (arena.isReady()) {
            sender.sendMessage("§a✔ Arena §e" + arenaName + " §ais now fully configured and ready for duels!");
        }
        return true;
    }

    @Command(
            names = {"duel arena list"},
            description = "List all duel arenas"
    )
    public boolean arenaList(CommandSender sender) {
        DuelGame game = getDuelGame();

        if (game.getArenas().isEmpty()) {
            sender.sendMessage("§cNo arenas configured yet. Use §e/duel arena create <name>§c.");
            return false;
        }

        sender.sendMessage("§6§lDuel Arenas:");
        game.getArenas().forEach(arena -> sender.sendMessage(
                "§7 • §e" + arena.getName()
                        + (arena.isReady() ? " §a[READY]" : " §c[INCOMPLETE — missing spawn(s)]")
        ));
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DuelGame getDuelGame() {
        return DuelGame.getInstance();
    }
}

