package games.sparking.altara.games.duels;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameManager;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.annotation.RegisterGame;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Duels game — 1v1 competitive matches with multiple kit options.
 *
 * <p>This class is the single source of truth for all active matches and pending
 * duel requests.  All other state is delegated to {@link DuelMatch}.
 *
 * <p>The event pipeline (damage, death prevention, item drops, etc.) lives in
 * the {@code module/} subpackage and is wired up via {@link games.sparking.altara.framework.GameScanner}.
 */
@RegisterGame(id = "duels")
public class DuelGame implements Game {

    // -------------------------------------------------------------------------
    // Singleton access (set in constructor, safe because plugin enables on main thread)
    // -------------------------------------------------------------------------

    @Getter
    private static DuelGame instance;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** player UUID → active match */
    private final Map<UUID, DuelMatch> playerMatches = new ConcurrentHashMap<>();

    /** target UUID → requester UUID (pending duel requests) */
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();

    /** target UUID → expiry timestamp (ms) */
    private final Map<UUID, Long> requestExpiry = new ConcurrentHashMap<>();

    /** All registered arenas */
    private final List<DuelArena> arenas = new ArrayList<>();

    /** All registered kits, keyed by id */
    private final Map<String, DuelKit> kits = new LinkedHashMap<>();

    private static final long REQUEST_TTL_MS = 30_000L;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DuelGame() {
        instance = this;

        // Register the default built-in kits
        registerKit(DuelKit.CLASSIC);
        registerKit(DuelKit.BOXING);
        registerKit(DuelKit.SUMO);
        registerKit(DuelKit.ARCHER);
    }

    // -------------------------------------------------------------------------
    // Game interface
    // -------------------------------------------------------------------------

    @Override
    public String id() {
        return "duels";
    }

    @Override
    public void start() {
        // Nothing to do on plugin enable — arenas are added externally
    }

    @Override
    public void stop() {
        // End every active match without a winner (server shutting down)
        new HashSet<>(playerMatches.values()).forEach(match ->
                endMatch(match, null, "§cServer shutting down."));
        playerMatches.clear();
        pendingRequests.clear();
        requestExpiry.clear();
    }

    @Override
    public GameState getState(UUID player) {
        return playerMatches.containsKey(player) ? GameState.PLAYING : GameState.LOBBY;
    }

    @Override
    public boolean isActive(UUID player) {
        return playerMatches.containsKey(player);
    }

    // -------------------------------------------------------------------------
    // Arenas & Kits
    // -------------------------------------------------------------------------

    public void addArena(DuelArena arena) {
        arenas.add(arena);
    }

    public Optional<DuelArena> getArena(String name) {
        return arenas.stream().filter(a -> a.getName().equalsIgnoreCase(name)).findFirst();
    }

    public List<DuelArena> getArenas() {
        return Collections.unmodifiableList(arenas);
    }

    /**
     * Returns the first arena not currently in use, or empty if all arenas are busy
     * or none exist.
     */
    public Optional<DuelArena> getAvailableArena() {
        Set<DuelArena> inUse = new HashSet<>();
        for (DuelMatch match : playerMatches.values()) {
            inUse.add(match.getArena());
        }
        return arenas.stream()
                .filter(DuelArena::isReady)
                .filter(a -> !inUse.contains(a))
                .findFirst();
    }

    public void registerKit(DuelKit kit) {
        kits.put(kit.getId().toLowerCase(), kit);
    }

    public Optional<DuelKit> getKit(String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase()));
    }

    public Collection<DuelKit> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    // -------------------------------------------------------------------------
    // Duel requests
    // -------------------------------------------------------------------------

    /**
     * Sends a duel request from {@code requester} to {@code target}.
     * The target has 30 seconds to accept via {@link #acceptRequest}.
     */
    public void sendRequest(Player requester, Player target, DuelKit kit) {
        if (isActive(requester.getUniqueId())) {
            requester.sendMessage("§cYou are already in a duel!");
            return;
        }
        if (isActive(target.getUniqueId())) {
            requester.sendMessage("§c" + target.getName() + " is already in a duel!");
            return;
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());
        requestExpiry.put(target.getUniqueId(), System.currentTimeMillis() + REQUEST_TTL_MS);

        requester.sendMessage("§6§lDUEL §7» §eDuel request sent to §a" + target.getName()
                + " §7(" + kit.getDisplayName() + "§7).");
        target.sendMessage("");
        target.sendMessage("§6§lDUEL §7» §e" + requester.getName() + " §7has challenged you to a"
                + " §a" + kit.getDisplayName() + " §7duel!");
        target.sendMessage("§7Type §a/duel accept §7to accept, or §c/duel deny §7to refuse.");
        target.sendMessage("§7This request expires in §e30 seconds§7.");
        target.sendMessage("");
    }

    /**
     * Attempts to start the match after the target accepts the request.
     *
     * @param target the player who received the request
     * @param kit    the kit to use for the match
     * @return true if the match was started successfully
     */
    public boolean acceptRequest(Player target, DuelKit kit) {
        UUID requesterUuid = pendingRequests.remove(target.getUniqueId());
        Long expiry = requestExpiry.remove(target.getUniqueId());

        if (requesterUuid == null) {
            target.sendMessage("§cYou have no pending duel requests.");
            return false;
        }
        if (expiry == null || System.currentTimeMillis() > expiry) {
            target.sendMessage("§cThat duel request has expired.");
            return false;
        }

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester == null) {
            target.sendMessage("§cThe player who challenged you is no longer online.");
            return false;
        }

        Optional<DuelArena> arena = getAvailableArena();
        if (arena.isEmpty()) {
            target.sendMessage("§cNo arenas are available right now. Please try again later.");
            requester.sendMessage("§c" + target.getName() + " tried to accept your duel but no arenas are available.");
            return false;
        }

        startMatch(requester, target, arena.get(), kit);
        return true;
    }

    /** Removes a pending request sent TO the given target. */
    public boolean denyRequest(Player target) {
        UUID requesterUuid = pendingRequests.remove(target.getUniqueId());
        requestExpiry.remove(target.getUniqueId());

        if (requesterUuid == null) {
            target.sendMessage("§cYou have no pending duel requests.");
            return false;
        }

        target.sendMessage("§7You denied the duel request.");
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            requester.sendMessage("§c" + target.getName() + " denied your duel request.");
        }
        return true;
    }

    public boolean hasPendingRequest(UUID targetUuid) {
        Long expiry = requestExpiry.get(targetUuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            pendingRequests.remove(targetUuid);
            requestExpiry.remove(targetUuid);
            return false;
        }
        return pendingRequests.containsKey(targetUuid);
    }

    // -------------------------------------------------------------------------
    // Match lifecycle
    // -------------------------------------------------------------------------

    /**
     * Creates and starts a new match between two players on the given arena with
     * the given kit.  Registers both players with the {@link GameManager}.
     */
    public void startMatch(Player p1, Player p2, DuelArena arena, DuelKit kit) {
        DuelMatch match = new DuelMatch(p1.getUniqueId(), p2.getUniqueId(), arena, kit);
        match.initialize(p1, p2);

        playerMatches.put(p1.getUniqueId(), match);
        playerMatches.put(p2.getUniqueId(), match);

        GameManager.getInstance().setPlayerGame(p1.getUniqueId(), this);
        GameManager.getInstance().setPlayerGame(p2.getUniqueId(), this);
    }

    /**
     * Ends a match, announces the result, restores inventories, and unregisters
     * both players from the {@link GameManager}.
     *
     * @param match        the match to end
     * @param winnerUuid   the winner's UUID, or {@code null} for a draw / no winner
     * @param endReason    optional reason message sent to both players (may be empty)
     */
    public void endMatch(DuelMatch match, UUID winnerUuid, String endReason) {
        // Remove from tracking first to prevent re-entrant calls
        playerMatches.remove(match.getPlayer1());
        playerMatches.remove(match.getPlayer2());
        GameManager.getInstance().removePlayer(match.getPlayer1());
        GameManager.getInstance().removePlayer(match.getPlayer2());

        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());

        String p1Name = p1 != null ? p1.getName() : "Player 1";
        String p2Name = p2 != null ? p2.getName() : "Player 2";

        String winnerName;
        String loserName;

        if (winnerUuid == null) {
            winnerName = null;
            loserName = null;
        } else if (winnerUuid.equals(match.getPlayer1())) {
            winnerName = p1Name;
            loserName = p2Name;
        } else {
            winnerName = p2Name;
            loserName = p1Name;
        }

        long duration = match.getDurationSeconds();

        // Restore & message each player
        for (Player p : new Player[]{p1, p2}) {
            if (p == null) continue;
            match.restore(p);

            p.sendMessage("");
            if (!endReason.isEmpty()) {
                p.sendMessage("§6§lDUEL §7» " + endReason);
            } else if (winnerName == null) {
                p.sendMessage("§6§lDUEL §7» §eThe match ended in a draw.");
            } else if (p.getName().equals(winnerName)) {
                p.sendMessage("§6§lDUEL §7» §aYou won the duel against §e" + loserName + "§a!");
            } else {
                p.sendMessage("§6§lDUEL §7» §cYou lost the duel against §e" + winnerName + "§c.");
            }
            p.sendMessage("§7Duration: §e" + formatDuration(duration));
            p.sendMessage("");
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public DuelMatch getMatch(UUID playerUuid) {
        return playerMatches.get(playerUuid);
    }

    public int getActiveMatchCount() {
        return playerMatches.size() / 2;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String formatDuration(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (mins > 0) return mins + "m " + secs + "s";
        return secs + "s";
    }
}

