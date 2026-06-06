package games.sparking.altara.games.skywars;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameModule;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.TeamGame;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.module.chest.ChestFillerModule;
import games.sparking.altara.framework.module.chest.LootItem;
import games.sparking.altara.framework.module.chest.LootTable;
import games.sparking.altara.framework.module.spectator.SpectatorModule;
import games.sparking.altara.framework.module.team.GameTeam;
import games.sparking.altara.framework.module.team.TeamColor;
import games.sparking.altara.framework.module.team.TeamModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * SkyWars — Duos variant.
 *
 * <p>Players compete in 2-person teams on shared islands.
 * The last team with at least one surviving member wins.
 *
 * <h3>Chest loot</h3>
 * Uses the same {@link LootTable} as {@link SkyWarsSolosGame} but with
 * slightly more generous slot counts to reward partner coordination.
 *
 * <h3>Game flow</h3>
 * <ol>
 *   <li>Register: {@code gameManager.register(new SkyWarsDuosGame())}.</li>
 *   <li>Create teams via {@link #createTeam(TeamColor)} and assign players via
 *       {@link #assignTeam(UUID, GameTeam)}.</li>
 *   <li>Deaths move the player to spectator; a team is eliminated when all of
 *       its members are spectating.</li>
 *   <li>{@link #hasWinner()} returns {@code true} when only one team remains.</li>
 * </ol>
 */
public class SkyWarsDuosGame extends TeamGame {

    // -------------------------------------------------------------------------
    // Loot table
    // -------------------------------------------------------------------------

    /**
     * SkyWars duos loot table — identical items to solos but fills
     * 4–8 slots to accommodate two players per island.
     */
    public static final LootTable LOOT_TABLE = LootTable.builder()
            .slots(4, 8)
            // Weapons
            .add(LootItem.of(Material.WOODEN_SWORD).weight(10).build())
            .add(LootItem.of(Material.STONE_SWORD).weight(6).build())
            .add(LootItem.of(Material.IRON_SWORD).weight(2).build())
            // Armour
            .add(LootItem.of(Material.LEATHER_HELMET).weight(8).build())
            .add(LootItem.of(Material.LEATHER_CHESTPLATE).weight(7).build())
            .add(LootItem.of(Material.CHAINMAIL_HELMET).weight(4).build())
            .add(LootItem.of(Material.CHAINMAIL_CHESTPLATE).weight(3).build())
            .add(LootItem.of(Material.IRON_HELMET).weight(1).build())
            // Consumables
            .add(LootItem.of(Material.BREAD).amount(1, 4).weight(9).build())
            .add(LootItem.of(Material.GOLDEN_APPLE).weight(2).build())
            // Projectiles
            .add(LootItem.of(Material.BOW).weight(3).build())
            .add(LootItem.of(Material.ARROW).amount(4, 16).weight(7).build())
            // Blocks (bridging)
            .add(LootItem.of(Material.COBBLESTONE).amount(8, 32).weight(10).build())
            .build();

    // -------------------------------------------------------------------------
    // Modules
    // -------------------------------------------------------------------------

    private final ChestFillerModule chestFiller = new ChestFillerModule(LOOT_TABLE);
    private final SpectatorModule   spectator   = new SpectatorModule();
    private final TeamModule        teamGuard   = new TeamModule(this);

    public SkyWarsDuosGame() {
        super(2); // 2 players per team
    }

    @Override
    public String id() {
        return "skywars-duos";
    }

    @Override
    public List<GameModule> modules() {
        return List.of(teamGuard, chestFiller, spectator);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void start() {
        // Team creation and player assignment happen externally via the lobby /
        // match-making system.  Chest locations are provided by the map controller.
        // Example:
        //   GameTeam red = createTeam(TeamColor.RED);
        //   assignTeam(playerUuid, red);
        //   chestFiller.fillChests(mapController.getChestLocations());
    }

    @Override
    public void stop() {
        getActivePlayers().forEach(this::removePlayer);
        getSpectators().forEach(this::removePlayer);
        chestFiller.reset();
    }

    // -------------------------------------------------------------------------
    // Chest filling convenience pass-through
    // -------------------------------------------------------------------------

    /**
     * Eagerly fills all chests at the given locations.
     * Delegate from your map-initialisation code after island layouts are set.
     *
     * @param locations chest block locations to fill
     */
    public void fillChests(Collection<Location> locations) {
        chestFiller.fillChests(locations);
    }

    // -------------------------------------------------------------------------
    // Elimination logic
    // -------------------------------------------------------------------------

    /**
     * Transitions a killed player to spectator and checks whether their team
     * has been fully eliminated.
     */
    @GameEvent(value = PlayerDeathEvent.class, states = {GameState.PLAYING})
    public void onPlayerDeath(PlayerDeathEvent event, Game game, Player player, GameState state) {
        addSpectator(player);
        checkWinCondition();
    }

    /**
     * Treats a quit as an elimination.
     */
    @GameEvent(value = PlayerQuitEvent.class, states = {GameState.PLAYING})
    public void onPlayerQuit(PlayerQuitEvent event, Game game, Player player, GameState state) {
        removePlayer(player.getUniqueId());
        checkWinCondition();
    }

    // -------------------------------------------------------------------------
    // Win condition
    // -------------------------------------------------------------------------

    private void checkWinCondition() {
        if (!hasWinner()) return;

        getWinnerTeam().ifPresent(team -> {
            String memberNames = team.getMembers().stream()
                    .map(uuid -> {
                        Player p = Bukkit.getPlayer(uuid);
                        return p != null ? p.getName() : uuid.toString();
                    })
                    .reduce((a, b) -> a + " & " + b)
                    .orElse("Unknown");

            Bukkit.broadcast(
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                            "<gold><bold>SkyWars Duos <reset><gray>» <yellow>" + memberNames
                            + " <gray>(" + team.getDisplayName() + "<gray>) have won the game!"));
        });
    }
}

