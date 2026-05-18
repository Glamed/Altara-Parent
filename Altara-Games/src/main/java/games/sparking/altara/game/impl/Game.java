package games.sparking.altara.game.impl;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.GameState;
import games.sparking.altara.game.event.*;
import games.sparking.altara.game.kit.KitManager;
import games.sparking.altara.game.module.GameModule;
import games.sparking.altara.game.spectator.SpectatorManager;
import games.sparking.altara.world.AltaraWorld;
import games.sparking.altara.world.MapLoader;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.player.GamePlayerState;
import games.sparking.altara.game.team.GameTeam;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Core abstract class for all game types on the Altara network.
 *
 * <h2>Multiple games, one server</h2>
 * Many {@code Game} instances can run simultaneously on the same server. Each instance is
 * isolated by its unique {@link #instanceId} and tracked by the {@link GameManager}.
 *
 * <h2>Lifecycle</h2>
 * {@link GameState#PreLoad} → {@link GameState#Loading} → {@link GameState#Recruit}
 * → {@link GameState#Prepare} → {@link GameState#Live} → {@link GameState#End}
 * → {@link GameState#Dead}
 *
 * <h2>Map auto-loading</h2>
 * Override {@link #getMapType()} to return your game's map folder name (e.g. {@code "skywars"}).
 * The base {@link #onLoad()} will then call {@link MapLoader#loadRandom} automatically —
 * you don't need to write any loading boilerplate.
 *
 * <h2>Player elimination</h2>
 * Call {@link #eliminatePlayer(Player)} to eliminate a player. The base class handles
 * kit removal, spectator mode, and fires {@link #onPlayerEliminated(GamePlayer)} so
 * subclasses can broadcast a message or trigger win-condition checks.
 * Death by HP≤0 is detected automatically — no need for an {@code @EventHandler} in subclasses.
 *
 * <h2>Modules</h2>
 * Call {@link #addModule(GameModule)} inside {@link #onStart()} to attach pluggable
 * behaviours (e.g. {@link games.sparking.altara.game.module.MapCrumbleModule}).
 * Modules are enabled when the game goes {@code Live} and disabled on {@code Dead}.
 */
public abstract class Game implements Listener {

    // =========================================================================
    // Identity
    // =========================================================================

    @Getter private final UUID   instanceId = UUID.randomUUID();
    @Getter private final String shortId    = instanceId.toString().substring(0, 8).toUpperCase();

    // =========================================================================
    // State
    // =========================================================================

    @Getter private GameState state     = GameState.PreLoad;
    @Getter private long      startTime = -1;
    @Getter private long      endTime   = -1;

    // =========================================================================
    // Players & Teams
    // =========================================================================

    @Getter private final Map<UUID, GamePlayer> players = new ConcurrentHashMap<>();
    @Getter private final List<GameTeam>        teams   = new ArrayList<>();

    // =========================================================================
    // Internal
    // =========================================================================


    @Getter private final KitManager       kitManager       = new KitManager(this);
    @Getter private final SpectatorManager spectatorManager = new SpectatorManager(this);

    @Getter @lombok.Setter protected AltaraWorld arenaWorld;

    /** Modules registered via {@link #addModule(GameModule)}. Attached when Live, detached on Dead. */
    private final List<GameModule> modules = new ArrayList<>();

    // =========================================================================
    // Abstract metadata
    // =========================================================================

    public abstract String getName();
    public abstract String getDescription();
    public abstract int    getMinPlayers();
    public abstract int    getMaxPlayers();

    /**
     * Returns the map folder name used for auto-loading, or {@code null} to skip map loading.
     *
     * <p>When non-null, {@link #onLoad()} will call {@link MapLoader#loadRandom} automatically.
     * Override this instead of overriding {@link #onLoad()} unless you need custom load logic.
     *
     * @return map type string (e.g. {@code "skywars"}), or {@code null}
     */
    protected String getMapType() { return null; }

    // =========================================================================
    // Lobby spawn
    // =========================================================================

    protected Location getLobbySpawn() {
        return Bukkit.getWorlds().getFirst().getSpawnLocation();
    }

    // =========================================================================
    // Lifecycle hooks
    // =========================================================================

    /**
     * Called on {@link GameState#Loading}.
     *
     * <p>Default: if {@link #getMapType()} returns a non-null string, asynchronously loads a random
     * map of that type and then advances to {@link GameState#Recruit}. If {@code getMapType()} is
     * {@code null}, advances immediately to {@link GameState#Recruit}.
     *
     * <p>Override for custom load logic (e.g. TeamSkyWars fallback maps). In that case, you must
     * call {@code setState(GameState.Recruit)} yourself when loading is done.
     */
    protected void onLoad() {
        String mapType = getMapType();
        if (mapType == null) {
            setState(GameState.Recruit);
            return;
        }
        if (!MapLoader.hasAnyMap(mapType)) {
            AltaraPaper.getPaperInstance().getLogger()
                    .warning("[" + getName() + "] No maps found for '" + mapType + "' — starting without a world.");
            setState(GameState.Recruit);
            return;
        }
        MapLoader.loadRandom(mapType, getShortId())
                .thenAcceptAsync(world -> {
                    setArenaWorld(world);
                    AltaraPaper.getPaperInstance().getLogger()
                            .info("[" + getName() + "] Loaded map '" + world.getMapName()
                                    + "' (instance " + getShortId() + ")");
                    setState(GameState.Recruit);
                }, r -> Bukkit.getScheduler().runTask(AltaraPaper.getPaperInstance(), r))
                .exceptionally(err -> {
                    AltaraPaper.getPaperInstance().getLogger()
                            .severe("[" + getName() + "] Map load failed: " + err.getMessage());
                    Bukkit.getScheduler().runTask(AltaraPaper.getPaperInstance(),
                            () -> setState(GameState.Recruit));
                    return null;
                });
    }

    /**
     * Called on {@link GameState#Recruit}.
     *
     * <p>Default: broadcasts the map name (if a world is loaded) and gives a kit-selector
     * item to every player (if kits are registered). Call {@code super.onRecruit()} to keep
     * this behaviour when overriding.
     */
    protected void onRecruit() {
        if (arenaWorld != null) broadcast(arenaWorld.getFormattedName());
        if (!kitManager.getKits().isEmpty()) {
            getPlayers().values().forEach(gp -> {
                Player p = gp.getPlayer();
                if (p != null) kitManager.giveSelectorItem(p);
            });
        }
    }

    /** Called on {@link GameState#Prepare}. Run countdown, freeze players, etc. */
    protected void onPrepare() {}

    /** Called on {@link GameState#Live}. Give kits, assign teams, start logic. */
    protected abstract void onStart();

    /**
     * Called on {@link GameState#End}.
     *
     * <p>Default: schedules {@link #destroy()} after 5 seconds. Override (and optionally call
     * {@code super.onEnd()}) to show results before cleaning up.
     */
    protected void onEnd() {
        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), this::destroy, 100L);
    }

    /**
     * Called on {@link GameState#Dead}.
     *
     * <p>Default: unloads {@link #arenaWorld} if one is set. Call {@code super.onDead()} after
     * game-specific cleanup (entity removal, block restoration, etc.).
     */
    protected void onDead() {
        if (arenaWorld != null) {
            MapLoader.unload(arenaWorld.getWorld()).thenRun(() ->
                    AltaraPaper.getPaperInstance().getLogger()
                            .info("[" + getName() + "] World unloaded for instance " + getShortId()));
            setArenaWorld(null);
        }
    }

    /**
     * Called after a player joins this game.
     *
     * <p>Default: gives a kit-selector item if the game is currently recruiting.
     * Call {@code super.onPlayerJoin(gp)} to keep this behaviour when overriding.
     */
    protected void onPlayerJoin(GamePlayer gamePlayer) {
        if (isRecruiting() && !kitManager.getKits().isEmpty()) {
            Player p = gamePlayer.getPlayer();
            if (p != null) kitManager.giveSelectorItem(p);
        }
    }

    /** Called after a player has been removed from this game. */
    protected void onPlayerLeave(GamePlayer gamePlayer) {}

    // =========================================================================
    // Elimination
    // =========================================================================

    /**
     * Eliminates a player: marks them as {@link GamePlayerState#ELIMINATED}, removes their kit,
     * puts them into spectator mode, and fires {@link #onPlayerEliminated(GamePlayer)}.
     *
     * <p>Called automatically when a player's health reaches zero. You can also call this
     * directly for out-of-bounds kills, void damage, etc.
     *
     * @param player the player to eliminate
     */
    protected final void eliminatePlayer(Player player) {
        GamePlayer gp = getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isAlive()) return;
        gp.eliminate();
        kitManager.removeKit(player);
        enterSpectatorMode(gp);
        onPlayerEliminated(gp);
    }

    /**
     * Hook fired after a player has been eliminated.
     *
     * <p>{@link SoloGame} overrides this to call {@link SoloGame#checkWinCondition()}.
     * {@link TeamGame} overrides this to fire {@link TeamGame#onTeamEliminated(GameTeam)} if needed.
     * Game subclasses can override (calling {@code super}) to broadcast an elimination message.
     *
     * @param gp the eliminated {@link GamePlayer}
     */
    protected void onPlayerEliminated(GamePlayer gp) {}

    /**
     * Detects player death by checking health after a 1-tick delay (to let damage be applied).
     * Replaces the boilerplate {@code @EventHandler onPlayerDamage} that previously lived in every game.
     */
    @EventHandler
    public void handleDeath(EntityDamageEvent event) {
        if (!isLive()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPlayer(player)) return;
        GamePlayer gp = getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isAlive()) return;

        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPaperInstance(), () -> {
            if (player.isOnline() && player.getHealth() <= 0) eliminatePlayer(player);
        }, 1L);
    }

    // =========================================================================
    // Module system
    // =========================================================================

    /**
     * Registers a {@link GameModule} with this game.
     *
     * <p>Call inside {@link #onStart()}. The module will be enabled automatically after
     * {@code onStart()} returns. All modules are disabled when the game goes {@code Dead}.
     *
     * @param module the module to register
     * @param <M>    module type
     * @return the same module (for chaining)
     */
    protected final <M extends GameModule> M addModule(M module) {
        modules.add(module);
        return module;
    }

    // =========================================================================
    // Start condition
    // =========================================================================

    public boolean canStart() {
        return players.size() >= getMinPlayers();
    }

    // =========================================================================
    // State management
    // =========================================================================

    public final void setState(GameState newState) {
        GameState oldState = this.state;
        if (oldState == newState) return;

        GameStateChangeEvent event = new GameStateChangeEvent(this, oldState, newState);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        this.state = newState;

        switch (newState) {
            case Loading -> {
                Bukkit.getPluginManager().registerEvents(this, AltaraPaper.getPaperInstance());
                kitManager.initialize();
                onLoad();
            }
            case Recruit  -> onRecruit();
            case Prepare  -> onPrepare();
            case Live     -> {
                startTime = System.currentTimeMillis();
                onStart();
                modules.forEach(m -> m.attach(this));   // attach after onStart so addModule() works inside
                Bukkit.getPluginManager().callEvent(new GameStartEvent(this));
            }
            case End      -> {
                endTime = System.currentTimeMillis();
                onEnd();
                Bukkit.getPluginManager().callEvent(new GameEndEvent(this));
            }
            case Dead     -> {
                modules.forEach(GameModule::detach);
                modules.clear();
                kitManager.cleanup();
                HandlerList.unregisterAll(this);
                evacuatePlayers();
                onDead();
                GameManager.getInstance().destroyGame(instanceId);
            }
        }
    }

    protected final void endGame() {
        if (state == GameState.Live) setState(GameState.End);
    }

    public final void destroy() {
        if (state != GameState.Dead) setState(GameState.Dead);
    }

    private void evacuatePlayers() {
        if (players.isEmpty()) return;
        Location lobby = getLobbySpawn();
        new ArrayList<>(players.values()).forEach(gp -> {
            Player p = gp.getPlayer();
            if (p != null && p.isOnline()) p.teleport(lobby);
            if (p != null) removePlayer(p);
        });
    }


    // =========================================================================
    // Player management
    // =========================================================================

    public final boolean addPlayer(Player player) {
        if (players.containsKey(player.getUniqueId())) return false;
        if (players.size() >= getMaxPlayers()) return false;
        if (GameManager.getInstance().getPlayerGame(player) != null) return false;

        GamePlayer gamePlayer = new GamePlayer(player, this);

        PlayerJoinGameEvent event = new PlayerJoinGameEvent(this, gamePlayer);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        players.put(player.getUniqueId(), gamePlayer);
        GameManager.getInstance().registerPlayerGame(player, this);
        onPlayerJoin(gamePlayer);
        return true;
    }

    public final boolean addSpectator(Player player) {
        if (GameManager.getInstance().getPlayerGame(player) != null) return false;

        GamePlayer gamePlayer = new GamePlayer(player, this);
        gamePlayer.makeSpectator();

        players.put(player.getUniqueId(), gamePlayer);
        GameManager.getInstance().registerPlayerGame(player, this);
        spectatorManager.enterSpectator(gamePlayer);
        onPlayerJoin(gamePlayer);
        return true;
    }

    public final void enterSpectatorMode(GamePlayer gp) {
        spectatorManager.enterSpectator(gp);
    }

    public final boolean removePlayer(Player player) {
        GamePlayer gamePlayer = players.remove(player.getUniqueId());
        if (gamePlayer == null) return false;

        if (gamePlayer.isSpectating()) spectatorManager.exitSpectator(gamePlayer);

        teams.forEach(team -> team.removePlayer(player));
        GameManager.getInstance().unregisterPlayerGame(player);

        Bukkit.getPluginManager().callEvent(new PlayerLeaveGameEvent(this, gamePlayer));
        onPlayerLeave(gamePlayer);
        return true;
    }

    public boolean hasPlayer(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    public Optional<GamePlayer> getGamePlayer(Player player) {
        return Optional.ofNullable(players.get(player.getUniqueId()));
    }

    public List<Player> getAlivePlayers() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .map(GamePlayer::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public long getAliveCount() {
        return players.values().stream().filter(GamePlayer::isAlive).count();
    }

    // =========================================================================
    // Team management
    // =========================================================================

    public GameTeam addTeam(GameTeam team) {
        teams.add(team);
        return team;
    }

    public Optional<GameTeam> getTeamOf(Player player) {
        return teams.stream().filter(t -> t.hasPlayer(player)).findFirst();
    }

    // =========================================================================
    // Messaging
    // =========================================================================

    public void broadcast(String message) {
        players.values().forEach(gp -> gp.sendMessage(message));
    }

    public void forEachPlayer(Consumer<GamePlayer> action) {
        players.values().forEach(action);
    }

    // =========================================================================
    // State queries
    // =========================================================================

    public boolean isLive()       { return state == GameState.Live; }
    public boolean isRecruiting() { return state == GameState.Recruit; }
    public boolean isPreparing()  { return state == GameState.Prepare; }
    public boolean hasEnded()     { return state == GameState.End || state == GameState.Dead; }

    @Override
    public String toString() {
        return getName() + "#" + shortId + "[" + state + ", players=" + players.size() + "]";
    }
}
