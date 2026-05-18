package games.sparking.altara.game.module.compass;

import games.sparking.altara.game.module.GameModule;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides a <em>Tracking Compass</em> that always points toward the nearest valid target.
 *
 * <h2>Default behaviour</h2>
 * <ul>
 *   <li>Spectators (eliminated players) receive a compass item in their hotbar.</li>
 *   <li>While holding the compass an action-bar shows target name, distance and height delta.</li>
 *   <li><b>Left-click</b> teleports the spectator next to the nearest target (3 s cooldown).</li>
 *   <li>Compass cannot be dropped.</li>
 *   <li>Compass is removed from death drops.</li>
 * </ul>
 *
 * <h2>Customisation</h2>
 * <pre>{@code
 * addModule(new CompassModule()
 *         .setGiveCompassToAlive(true)          // give to alive players too
 *         .setGiveCompassToSpectators(false)    // don't auto-give item
 *         .addSupplier(myCustomEntrySupplier)); // extra targets
 * }</pre>
 *
 * <p><b>Session isolation:</b> only players registered in {@link #getGame()} are processed;
 * targets are drawn exclusively from alive players of this game.
 */
public class CompassModule extends GameModule {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final String    COMPASS_NAME_LEGACY = "§a§lTracking Compass"; // for item matching
    private static final Component COMPASS_NAME_COMP   = Component.text("Tracking Compass")
            .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
    private static final long                  SPECTATE_COOLDOWN_MS = 3_000L;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final List<Supplier<Collection<CompassEntry>>> suppliers        = new ArrayList<>();
    private final Map<UUID, Long>                          spectatorCooldowns = new HashMap<>();

    private boolean giveCompassToAlive       = false;
    private boolean giveCompassToSpectators  = true;

    // -------------------------------------------------------------------------
    // Configuration API
    // -------------------------------------------------------------------------

    /** If {@code true}, alive players also receive and track the compass. Default: {@code false}. */
    public CompassModule setGiveCompassToAlive(boolean b) {
        this.giveCompassToAlive = b;
        return this;
    }

    /** If {@code false}, no compass item is auto-placed in spectator inventories. Default: {@code true}. */
    public CompassModule setGiveCompassToSpectators(boolean b) {
        this.giveCompassToSpectators = b;
        return this;
    }

    /** Adds an extra source of {@link CompassEntry} objects. */
    public CompassModule addSupplier(Supplier<Collection<CompassEntry>> supplier) {
        suppliers.add(supplier);
        return this;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onEnable() {
        // Default supplier: alive players in this game session
        suppliers.add(() ->
                getGame().getAlivePlayers().stream()
                        .map(p -> new CompassEntry(
                                p,
                                p.getName(),
                                p.getName(),
                                getGame().getTeamOf(p).orElse(null)))
                        .collect(Collectors.toList())
        );
    }

    @Override
    protected void onDisable() {
        suppliers.clear();
        spectatorCooldowns.clear();
    }

    // -------------------------------------------------------------------------
    // Compass update
    // -------------------------------------------------------------------------

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FASTER) return;
        if (!getGame().isLive()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!getGame().hasPlayer(player)) continue;

            GamePlayer gp = getGame().getGamePlayer(player).orElse(null);
            if (gp == null) continue;

            boolean spectating = gp.isSpectating();
            if (!giveCompassToAlive && !spectating) continue;

            // Give item if needed
            if (giveCompassToSpectators && spectating) {
                ensureCompass(player);
            }

            // Find nearest non-cancelled target
            Optional<CompassEntry> nearest = stream()
                    .filter(e -> e.getEntity() != player)
                    .filter(e -> {
                        CompassAttemptTargetEvent attempt =
                                new CompassAttemptTargetEvent(player, e.getEntity());
                        Bukkit.getPluginManager().callEvent(attempt);
                        return !attempt.isCancelled();
                    })
                    .min(Comparator.comparingDouble(
                            e -> e.getEntity().getLocation().distanceSquared(player.getLocation())));

            nearest.ifPresent(target -> {
                Entity targetEntity = target.getEntity();
                player.setCompassTarget(targetEntity.getLocation());

                // Action bar while holding the compass
                if (isHoldingCompass(player)) {
                    double dist       = Math.sqrt(targetEntity.getLocation().distanceSquared(player.getLocation()));
                    double heightDiff = targetEntity.getLocation().getY() - player.getLocation().getY();
                    String teamColour = target.getTeam() != null
                            ? target.getTeam().getColor().prefix() : "§f";
                    player.sendActionBar(Component.text(
                            "§f§lNearest: " + teamColour + target.getDisplayName()
                                    + "  §f§lDist: " + teamColour + String.format("%.1f", dist)
                                    + "  §f§lHeight: " + teamColour + String.format("%.1f", heightDiff)));
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Interaction / item events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!getGame().hasPlayer(event.getPlayer())) return;
        if (!isCompassItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYou cannot drop the Tracking Compass.");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!getGame().hasPlayer(event.getEntity())) return;
        event.getDrops().removeIf(this::isCompassItem);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;

        Player player = event.getPlayer();
        if (!getGame().hasPlayer(player)) return;

        GamePlayer gp = getGame().getGamePlayer(player).orElse(null);
        if (gp == null || !gp.isSpectating()) return;
        if (!isHoldingCompass(player)) return;

        event.setCancelled(true);

        // Left-click: teleport to nearest target (with cooldown)
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            long now = System.currentTimeMillis();
            if (now - spectatorCooldowns.getOrDefault(player.getUniqueId(), 0L) < SPECTATE_COOLDOWN_MS) return;
            spectatorCooldowns.put(player.getUniqueId(), now);

            stream()
                    .min(Comparator.comparingDouble(
                            e -> e.getEntity().getLocation().distanceSquared(player.getLocation())))
                    .map(CompassEntry::getEntity)
                    .ifPresent(target -> player.teleport(target.getLocation().add(0, 1, 0)));
        }
        // Right-click: future menu hook (no-op for now)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void ensureCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCompassItem(item)) return;
        }
        player.getInventory().addItem(buildCompassItem());
    }

    private static ItemStack buildCompassItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        item.editMeta(meta -> meta.displayName(COMPASS_NAME_COMP));
        return item;
    }

    /** Checks whether the given item is the module's Tracking Compass. */
    public boolean isCompassItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        // Match via Adventure display name
        Component display = meta.displayName();
        if (display != null && display.equals(COMPASS_NAME_COMP)) return true;
        // Fallback: legacy name match (for items created before migration)
        //noinspection deprecation
        return meta.hasDisplayName() && COMPASS_NAME_LEGACY.equals(meta.getDisplayName());
    }

    private boolean isHoldingCompass(Player player) {
        return isCompassItem(player.getInventory().getItemInMainHand());
    }

    /** Returns a flat stream over all registered entry suppliers. */
    public Stream<CompassEntry> stream() {
        return suppliers.stream().map(Supplier::get).flatMap(Collection::stream);
    }
}





