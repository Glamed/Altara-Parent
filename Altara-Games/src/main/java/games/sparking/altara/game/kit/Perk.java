package games.sparking.altara.game.kit;

import games.sparking.altara.game.impl.Game;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * A single passive or active ability attached to a {@link Kit}.
 *
 * <p>Perks implement their logic via Bukkit {@code @EventHandler} methods (just add
 * {@code implements Listener} to the subclass) or by overriding {@link #apply(Player)}
 * and {@link #remove(Player)}.
 *
 * <p>Use {@link #hasPerk(Player)} inside event handlers to verify that the player who
 * triggered the event actually has this perk active.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class PerkNoFallDamage extends Perk implements Listener {
 *     public PerkNoFallDamage() {
 *         super("No Fall Damage", new String[]{ "§7You take no fall damage." });
 *     }
 *
 *     @EventHandler
 *     public void onFall(EntityDamageEvent event) {
 *         if (!(event.getEntity() instanceof Player p)) return;
 *         if (!hasPerk(p)) return;
 *         if (event.getCause() == EntityDamageEvent.DamageCause.FALL)
 *             event.setCancelled(true);
 *     }
 * }
 * }</pre>
 */
public abstract class Perk {

    // Set by Kit.setKit() when the perk is registered to its parent.
    @Getter private Kit kit;

    @Getter private final String name;
    @Getter private final String[] description;
    @Getter private final boolean visible;

    public Perk(String name, String[] description) {
        this(name, description, true);
    }

    public Perk(String name, String[] description, boolean visible) {
        this.name = name;
        this.description = description;
        this.visible = visible;
    }

    // =========================================================================
    // Internal – called by Kit constructor
    // =========================================================================

    void setKit(Kit kit) {
        this.kit = kit;
    }

    // =========================================================================
    // Convenience accessors
    // =========================================================================

    /**
     * Returns the {@link Game} that owns the {@link Kit} this perk belongs to.
     *
     * @throws IllegalStateException if this perk is not yet attached to a kit
     */
    public Game getGame() {
        if (kit == null) throw new IllegalStateException("Perk '" + name + "' is not attached to any kit.");
        return kit.getGame();
    }

    /**
     * @return {@code true} if the given player has the kit that contains this perk
     *         (i.e. they selected it and the game is running).
     */
    public boolean hasPerk(Player player) {
        return kit != null && kit.hasKit(player);
    }

    // =========================================================================
    // Lifecycle hooks
    // =========================================================================

    /**
     * Called when the owning kit is applied to a player at game start.
     * Use this to give potion effects, set properties, etc.
     * Default: no-op.
     */
    public void apply(Player player) {}

    /**
     * Called when a player leaves the game or the game ends.
     * Use this to clean up potion effects, flight flags, etc.
     * Default: no-op.
     */
    public void remove(Player player) {}

    /**
     * Called after this perk's listener has been registered with Bukkit.
     * Override to perform one-time setup that requires the game/event system to be live.
     */
    public void onRegister() {}

    /**
     * Called after this perk's listener has been unregistered from Bukkit.
     * Override to clean up any state that spans multiple players (e.g. placed blocks).
     */
    public void onUnregister() {}

    @Override
    public String toString() {
        return "Perk{" + name + "}";
    }
}

