package games.sparking.altara.game.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gives players a "Throwable TNT" item that, when clicked, launches a primed
 * {@link TNTPrimed} entity.
 *
 * <h2>Behaviour (matches Mineplex)</h2>
 * <ul>
 *   <li>When {@link #setThrowAndDrop(boolean) throwAndDrop} is {@code true}:
 *       <b>left-click = throw</b>, <b>right-click = drop in place</b>.</li>
 *   <li>Otherwise every click throws.</li>
 *   <li>500 ms per-player throw cooldown.</li>
 *   <li>The launched TNT is tracked so nearby game-players receive damage credit
 *       via {@link ExplosionPrimeEvent}.</li>
 * </ul>
 *
 * <p><b>Session isolation:</b> player handlers guard with {@link #getGame()}{@code .hasPlayer()};
 * explosion handlers only act on TNT entities spawned by this module instance.
 */
public class ThrowableTNTModule extends GameModule {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    private static final int  EXPLOSION_RADIUS = 14;
    private static final long THROW_COOLDOWN_MS = 500L;

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    private int     fuseTicks        = 60;    // 3 seconds
    private boolean throwAndDrop     = false;
    private double  throwStrength    = 1.3;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Maps a spawned TNT entity UUID → the player who threw it. */
    private final Map<UUID, UUID> throwers = new HashMap<>();

    /** Per-player throw cooldown (UUID → last-throw timestamp). */
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    /** Cached item, built in {@link #onEnable()}. */
    private ItemStack tntItem;

    // -------------------------------------------------------------------------
    // Configuration API
    // -------------------------------------------------------------------------

    /** Sets the fuse length in ticks. Default: {@code 60} (3 seconds). */
    public ThrowableTNTModule setFuseTicks(int ticks) {
        this.fuseTicks = ticks;
        return this;
    }

    /**
     * When {@code true}: left-click throws, right-click drops in place.
     * Default: {@code false} (every action throws).
     */
    public ThrowableTNTModule setThrowAndDrop(boolean throwAndDrop) {
        this.throwAndDrop = throwAndDrop;
        return this;
    }

    /** Sets the launch speed multiplier. Default: {@code 1.3}. */
    public ThrowableTNTModule setThrowStrength(double strength) {
        this.throwStrength = strength;
        return this;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onEnable() {
        tntItem = buildItem();
    }

    @Override
    protected void onDisable() {
        throwers.clear();
        cooldowns.clear();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a single-item stack of Throwable TNT.
     * Call {@code clone()} and {@code setAmount()} before giving it to a player or generator.
     */
    public ItemStack getTntItem() {
        // If onEnable hasn't run yet (module not attached), build on demand
        return tntItem != null ? tntItem : buildItem();
    }

    /** Returns {@code true} if {@code item} is a Throwable TNT stack from this module. */
    public boolean isTntItem(ItemStack item) {
        if (item == null || item.getType() != Material.TNT) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        // Match using legacy name stored in item (both throw-and-drop and plain variants)
        String name = meta.getDisplayName();
        return name.contains("Throwable TNT") || name.contains("Left Click - Throw");
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        if (!getGame().isLive()) return;

        Player player = event.getPlayer();
        if (!getGame().hasPlayer(player)) return;

        // Spectator guard
        if (getGame().getGamePlayer(player).map(gp -> !gp.isAlive()).orElse(true)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isTntItem(hand)) return;

        // Usable-block guard — don't intercept clicks on chests/doors etc.
        if (event.getClickedBlock() != null && isUsableBlock(event.getClickedBlock().getType())) return;

        // 500 ms cooldown
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < THROW_COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        event.setCancelled(true);

        // Decrement hand item
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK;

        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection();

        TNTPrimed tnt = player.getWorld().spawn(
                origin.clone().add(direction),
                TNTPrimed.class);
        tnt.setFuseTicks(fuseTicks);
        tnt.setSource(player);

        // throwAndDrop: left-click = throw with velocity, right-click = drop in place
        if (!throwAndDrop || isLeftClick) {
            Vector velocity = direction.clone()
                    .multiply(throwStrength)
                    .add(new Vector(0, 0.3, 0));  // upward component, matches Mineplex
            tnt.setVelocity(velocity);
        }
        // else: right-click drops the TNT at the player's feet → no velocity needed

        throwers.put(tnt.getUniqueId(), player.getUniqueId());
    }

    /**
     * When one of our TNT entities is about to explode, deal damage to all nearby
     * game-players — mirroring Mineplex's {@code ExplosionPrimeEvent} handler.
     */
    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        UUID throwerUUID = throwers.remove(event.getEntity().getUniqueId());
        if (throwerUUID == null) return;

        Player thrower = org.bukkit.Bukkit.getPlayer(throwerUUID);
        Location epicentre = event.getEntity().getLocation();

        // Deal knockback + damage to all alive game-players within radius
        for (Player nearby : epicentre.getWorld().getNearbyPlayers(epicentre, EXPLOSION_RADIUS)) {
            if (!getGame().hasPlayer(nearby)) continue;
            if (!getGame().getGamePlayer(nearby).map(gp -> gp.isAlive()).orElse(false)) continue;

            // Apply vanilla-style explosion damage (Mineplex uses their condition system;
            // we use Bukkit damage so the normal kill-credit chain fires).
            double dist = nearby.getLocation().distance(epicentre);
            double damage = Math.max(1, 10 - dist * 0.5);  // falls off with distance
            nearby.damage(damage, thrower);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!getGame().hasPlayer(event.getPlayer())) return;
        if (!isTntItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ItemStack buildItem() {
        ItemStack item = new ItemStack(Material.TNT);
        item.editMeta(meta -> {
            Component name;
            if (throwAndDrop) {
                name = Component.text()
                        .append(Component.text("Left Click - Throw")
                                .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                        .append(Component.text(" / ").color(NamedTextColor.WHITE))
                        .append(Component.text("Right Click - Drop")
                                .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                        .build();
            } else {
                name = Component.text("Throwable TNT").color(NamedTextColor.YELLOW);
            }
            meta.displayName(name);
        });
        return item;
    }

    /** Returns {@code true} for blocks that open a GUI or have right-click actions. */
    private static boolean isUsableBlock(Material m) {
        return switch (m) {
            case CHEST, TRAPPED_CHEST, ENDER_CHEST, SHULKER_BOX,
                 CRAFTING_TABLE, FURNACE, ANVIL, ENCHANTING_TABLE,
                 OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR,
                 ACACIA_DOOR, DARK_OAK_DOOR, IRON_DOOR,
                 OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR,
                 JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR -> true;
            default -> false;
        };
    }
}

