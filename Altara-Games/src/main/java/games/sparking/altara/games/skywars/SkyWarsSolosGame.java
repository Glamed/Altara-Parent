package games.sparking.altara.games.skywars;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameModule;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.SoloGame;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.module.chest.ChestFillerModule;
import games.sparking.altara.framework.module.chest.LootItem;
import games.sparking.altara.framework.module.chest.LootTable;
import games.sparking.altara.framework.module.spectator.SpectatorModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * SkyWars — Solo variant.
 *
 * <p>Every player fights for themselves on their own island.
 * The last player standing wins.
 *
 * <h3>Chest loot</h3>
 * The shared {@link ChestFillerModule} is pre-configured with
 * {@link #LOOT_TABLE}, a balanced SkyWars loot pool.
 * Call {@link #fillChests(Collection)} from whichever code initialises the
 * map to eagerly populate all chest locations before players spawn.
 *
 * <h3>Game flow</h3>
 * <ol>
 *   <li>Register this game with {@code gameManager.register(new SkyWarsSolosGame())}.</li>
 *   <li>Add players via {@link #setPlayerState(UUID, GameState)} with
 *       {@link GameState#PLAYING}.</li>
 *   <li>When a player dies they automatically become a spectator.</li>
 *   <li>When only one player remains {@link #hasWinner()} returns {@code true}.</li>
 * </ol>
 */
public class SkyWarsSolosGame extends SoloGame {

    // -------------------------------------------------------------------------
    // Loot table
    // -------------------------------------------------------------------------

    /**
     * Default SkyWars solo loot table.
     *
     * <p>Weights are relative — higher weight = appears more often.
     */
    public static final LootTable LOOT_TABLE = LootTable.builder()
            .slots(3, 7)
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
            .add(LootItem.of(Material.BREAD).amount(1, 3).weight(9).build())
            .add(LootItem.of(Material.GOLDEN_APPLE).weight(2).build())
            // Projectiles
            .add(LootItem.of(Material.BOW).weight(3).build())
            .add(LootItem.of(Material.ARROW).amount(4, 12).weight(7).build())
            // Blocks (bridging)
            .add(LootItem.of(Material.COBBLESTONE).amount(8, 24).weight(10).build())
            .build();

    // -------------------------------------------------------------------------
    // Modules
    // -------------------------------------------------------------------------

    private final ChestFillerModule chestFiller = new ChestFillerModule(LOOT_TABLE);
    private final SpectatorModule   spectator   = new SpectatorModule();

    @Override
    public String id() {
        return "skywars-solos";
    }

    @Override
    public List<GameModule> modules() {
        return List.of(chestFiller, spectator);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void start() {
        // Map setup happens externally; chest filling is triggered by the
        // map controller once island locations are known.
        // Example: chestFiller.fillChests(mapController.getChestLocations());
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
     * Transitions a killed player into spectator mode and checks for a winner.
     */
    @GameEvent(value = PlayerDeathEvent.class, states = {GameState.PLAYING})
    public void onPlayerDeath(PlayerDeathEvent event, Game game, Player player, GameState state) {
        addSpectator(player);
        checkWinCondition();
    }

    /**
     * Treats a player quitting mid-game the same as dying.
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

        getWinner().ifPresent(winnerId -> {
            Player winner = org.bukkit.Bukkit.getPlayer(winnerId);
            String name = winner != null ? winner.getName() : winnerId.toString();
            org.bukkit.Bukkit.broadcast(
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                            "<gold><bold>SkyWars Solos <reset><gray>» <yellow>" + name + " <gray>has won the game!"));
        });
    }
}

