package games.sparking.altara.games.survivalgames;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameModule;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.TeamGame;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.module.chest.ChestFillerModule;
import games.sparking.altara.framework.module.chest.LootItem;
import games.sparking.altara.framework.module.chest.LootTable;
import games.sparking.altara.framework.module.spectator.SpectatorModule;
import games.sparking.altara.framework.module.team.TeamModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.List;

/**
 * Survival Games — Duos variant.
 *
 * <p>Pairs of players scavenge a shared world together; the last duo with at
 * least one surviving member wins. Friendly fire between partners is blocked
 * by the included {@link TeamModule}.
 *
 * <h3>Loot</h3>
 * Shares the Survival Games item pool ({@link #LOOT_TABLE}) but fills
 * 3–6 slots per chest to provide enough supplies for two players.
 *
 * <h3>Wiring</h3>
 * <pre>
 * gameManager.register(new SurvivalGamesDuosGame());
 * </pre>
 */
public class SurvivalGamesDuosGame extends TeamGame {

    // -------------------------------------------------------------------------
    // Loot table
    // -------------------------------------------------------------------------

    /**
     * Survival Games duos loot table.
     *
     * <p>Same item pool as {@link SurvivalGamesSolosGame} but with 3–6 slots
     * so both partners can expect to find usable gear.
     */
    public static final LootTable LOOT_TABLE = LootTable.builder()
            .slots(3, 6)
            // Food
            .add(LootItem.of(Material.BREAD).amount(1, 4).weight(12).build())
            .add(LootItem.of(Material.COOKED_BEEF).amount(1, 3).weight(10).build())
            .add(LootItem.of(Material.APPLE).amount(1, 3).weight(11).build())
            .add(LootItem.of(Material.GOLDEN_APPLE).weight(2).build())
            // Weapons
            .add(LootItem.of(Material.WOODEN_SWORD).weight(8).build())
            .add(LootItem.of(Material.STONE_SWORD).weight(5).build())
            .add(LootItem.of(Material.IRON_SWORD).weight(1).build())
            // Tools
            .add(LootItem.of(Material.WOODEN_AXE).weight(6).build())
            .add(LootItem.of(Material.STONE_AXE).weight(3).build())
            .add(LootItem.of(Material.WOODEN_PICKAXE).weight(5).build())
            // Armour
            .add(LootItem.of(Material.LEATHER_HELMET).weight(7).build())
            .add(LootItem.of(Material.LEATHER_CHESTPLATE).weight(6).build())
            .add(LootItem.of(Material.LEATHER_LEGGINGS).weight(6).build())
            .add(LootItem.of(Material.LEATHER_BOOTS).weight(7).build())
            .add(LootItem.of(Material.CHAINMAIL_CHESTPLATE).weight(2).build())
            .add(LootItem.of(Material.IRON_HELMET).weight(1).build())
            // Projectiles
            .add(LootItem.of(Material.BOW).weight(3).build())
            .add(LootItem.of(Material.ARROW).amount(4, 16).weight(6).build())
            // Utility
            .add(LootItem.of(Material.TORCH).amount(4, 16).weight(8).build())
            .add(LootItem.of(Material.FLINT_AND_STEEL).weight(2).build())
            .build();

    // -------------------------------------------------------------------------
    // Modules
    // -------------------------------------------------------------------------

    private final ChestFillerModule chestFiller = new ChestFillerModule(LOOT_TABLE);
    private final SpectatorModule   spectator   = new SpectatorModule();
    private final TeamModule        teamGuard   = new TeamModule(this);

    public SurvivalGamesDuosGame() {
        super(2); // 2 players per team
    }

    @Override
    public String id() {
        return "survivalgames-duos";
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
        // Team creation and player assignment happen externally.
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
     *
     * @param locations chest block locations to fill
     */
    public void fillChests(Collection<Location> locations) {
        chestFiller.fillChests(locations);
    }

    // -------------------------------------------------------------------------
    // Elimination logic
    // -------------------------------------------------------------------------

    @GameEvent(value = PlayerDeathEvent.class, states = {GameState.PLAYING})
    public void onPlayerDeath(PlayerDeathEvent event, Game game, Player player, GameState state) {
        addSpectator(player);
        checkWinCondition();
    }

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
                            "<dark_green><bold>Survival Games <reset><gray>» <green>" + memberNames
                            + " <gray>(" + team.getDisplayName() + "<gray>) have survived and won the game!"));
        });
    }
}

