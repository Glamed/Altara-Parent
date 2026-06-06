package games.sparking.altara.games.survivalgames;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameModule;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.SoloGame;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.module.chest.ChestFillerModule;
import games.sparking.altara.framework.module.chest.LootItem;
import games.sparking.altara.framework.module.chest.LootTable;
import games.sparking.altara.framework.module.spectator.SpectatorModule;
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
 * Survival Games — Solo variant.
 *
 * <p>Players scatter across a large map, scavenging from scattered chests
 * and fighting to be the last one standing.  The game follows the classic
 * Survival-Games / Hunger-Games formula:
 * <ul>
 *   <li>Players begin at spread-out spawn points with no items.</li>
 *   <li>Chests scattered across the map hold the loot pool defined in
 *       {@link #LOOT_TABLE}.</li>
 *   <li>Each death immediately converts the player to a spectator.</li>
 *   <li>The last survivor is declared the winner.</li>
 * </ul>
 *
 * <h3>Loot philosophy</h3>
 * Survival-Games chests lean heavier on food and early-game tools than
 * SkyWars to suit longer match pacing.
 *
 * <h3>Wiring</h3>
 * <pre>
 * gameManager.register(new SurvivalGamesSolosGame());
 * </pre>
 */
public class SurvivalGamesSolosGame extends SoloGame {

    // -------------------------------------------------------------------------
    // Loot table
    // -------------------------------------------------------------------------

    /**
     * Survival Games solo loot table.
     *
     * <p>Balanced toward early-game scavenging with plentiful food,
     * tools, and modest weapons — high-tier gear is rare.
     */
    public static final LootTable LOOT_TABLE = LootTable.builder()
            .slots(2, 5)
            // Food (plentiful — survival is tight early on)
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

    @Override
    public String id() {
        return "survivalgames-solos";
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
        // Chest locations are provided by the map controller once the world is loaded.
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

        getWinner().ifPresent(winnerId -> {
            Player winner = Bukkit.getPlayer(winnerId);
            String name = winner != null ? winner.getName() : winnerId.toString();
            org.bukkit.Bukkit.broadcast(
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                            "<dark_green><bold>Survival Games <reset><gray>» <green>" + name + " <gray>has survived and won the game!"));
        });
    }
}

