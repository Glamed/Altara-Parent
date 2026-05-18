package games.sparking.altara.game.module.generator;

import games.sparking.altara.game.module.GameModule;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a collection of {@link Generator}s for a game session.
 *
 * <p>Generators periodically produce an item (shown as an ArmorStand floating display)
 * which is auto-collected by the first player who walks close enough.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * GeneratorType GOLD = new GeneratorType(
 *         new ItemStack(Material.GOLD_INGOT), 20_000L, "Gold",
 *         ChatColor.GOLD, Color.YELLOW, false);
 *
 * GeneratorModule generators = addModule(new GeneratorModule());
 * for (Location loc : arena.getData("generator")) {
 *     generators.addGenerator(new Generator(GOLD, loc));
 * }
 * }</pre>
 *
 * <p><b>Session isolation:</b> block-break and armour-stand events are filtered by the
 * specific {@link Generator} instances owned by this module; player events additionally
 * check {@link #getGame()}{@code .hasPlayer()}.
 */
public class GeneratorModule extends GameModule {

    private final List<Generator> generators = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Registers a generator with this module. Returns {@code this} for chaining. */
    public GeneratorModule addGenerator(Generator generator) {
        generators.add(generator);
        return this;
    }

    /** An unmodifiable view of all registered generators. */
    public List<Generator> getGenerators() {
        return Collections.unmodifiableList(generators);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onDisable() {
        // Remove any floating ArmorStands that are still alive
        for (Generator g : generators) {
            ArmorStand holder = g.getHolder();
            if (holder != null && holder.isValid()) holder.remove();
        }
        generators.clear();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (!getGame().isLive()) return;

        if (event.getType() == UpdateType.FAST) {
            for (Generator g : generators) {
                g.checkSpawn();
                g.checkCollect();
                g.updateName();
            }
        } else if (event.getType() == UpdateType.TICK) {
            for (Generator g : generators) {
                g.animateHolder();
            }
        }
    }

    /** Initialise spawn timers so items don't immediately appear at game start. */
    @EventHandler
    public void onGameStart(games.sparking.altara.game.event.GameStartEvent event) {
        if (!event.getGame().equals(getGame())) return;
        generators.forEach(Generator::setLastCollect);
    }

    /** Prevent players from breaking the block beneath a generator. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!getGame().hasPlayer(player)) return;

        Block block = event.getBlock();
        for (Generator g : generators) {
            if (g.getBlock().equals(block)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot break a generator block.");
                return;
            }
        }
    }

    /** Prevent players from interacting with generator ArmorStands. */
    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!getGame().hasPlayer(event.getPlayer())) return;
        for (Generator g : generators) {
            if (g.getHolder() != null && g.getHolder().equals(event.getRightClicked())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /** Prevent generator ArmorStands from taking damage. */
    @EventHandler
    public void onArmorStandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        for (Generator g : generators) {
            if (g.getHolder() != null && g.getHolder().equals(stand)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}

