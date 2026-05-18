package games.sparking.altara.game.module;

import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.team.GameTeam;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lets players who disconnect mid-game come back and restore their state.
 *
 * <p>When an alive player quits their health (and optionally full inventory) is saved.
 * Because the {@link games.sparking.altara.game.impl.Game} keeps offline players in its
 * player map, no re-add is needed — on reconnect this module simply restores their
 * attributes so they can continue playing.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * addModule(new RejoinModule());                     // health only
 * addModule(new RejoinModule().setSaveInventory(true)); // health + inventory
 * }</pre>
 *
 * <p><b>Session isolation:</b> only players registered in {@link #getGame()} trigger saves.
 */
public class RejoinModule extends GameModule {

    private boolean saveInventory = false;

    private final Map<UUID, RejoinData> rejoinData = new HashMap<>();

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** Whether to persist and restore the player's full inventory on rejoin. Default: {@code false}. */
    public RejoinModule setSaveInventory(boolean save) {
        this.saveInventory = save;
        return this;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onDisable() {
        rejoinData.clear();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * When an alive player quits, snapshot their state so it can be restored on rejoin.
     * The player is intentionally left in the game's player map so that the game's
     * alive count and team membership stay intact during the brief disconnect.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!getGame().isLive()) return;

        Player player = event.getPlayer();
        if (!getGame().hasPlayer(player)) return;

        GamePlayer gp = getGame().getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isAlive()) return;

        rejoinData.put(player.getUniqueId(), new RejoinData(
                player.getHealth(),
                saveInventory ? player.getInventory().getContents().clone()      : null,
                saveInventory ? player.getInventory().getArmorContents().clone() : null,
                gp.getTeam()
        ));
    }

    /**
     * When a player rejoins while their data is still held, restore their state.
     * No re-add is required because they never left the game's player map.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!getGame().isLive()) return;

        Player player = event.getPlayer();
        RejoinData data = rejoinData.remove(player.getUniqueId());
        if (data == null) return;

        // Sanity: make sure they're still in this game (could have been eliminated while offline)
        if (!getGame().hasPlayer(player)) return;

        // Restore health (clamp to current max)
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(data.health(), maxHealth));

        // Restore inventory
        if (saveInventory && data.inventoryContents() != null) {
            player.getInventory().setContents(data.inventoryContents());
            player.getInventory().setArmorContents(data.armorContents());
        }

        // Announce rejoin to the game
        getGame().broadcast("§e" + player.getName() + " §7has rejoined the game.");
    }

    // -------------------------------------------------------------------------
    // Data record
    // -------------------------------------------------------------------------

    private record RejoinData(
            double       health,
            ItemStack[]  inventoryContents,
            ItemStack[]  armorContents,
            GameTeam     team
    ) {}
}

