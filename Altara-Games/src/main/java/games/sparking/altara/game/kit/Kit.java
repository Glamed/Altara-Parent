package games.sparking.altara.game.kit;

import games.sparking.altara.game.impl.Game;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a playable kit in any {@link Game}.
 *
 * <p>A {@code Kit} is a named collection of {@link Perk}s plus optional starter items,
 * tied to a specific game instance.  When a player selects a kit and the game goes
 * {@link games.sparking.altara.game.GameState#Live}, {@link #apply(Player)} is called
 * which:
 * <ol>
 *   <li>Fires a {@link games.sparking.altara.game.kit.event.KitApplyEvent}</li>
 *   <li>Invokes {@link Perk#apply(Player)} for every perk</li>
 *   <li>Calls {@link #giveItems(Player)}</li>
 * </ol>
 *
 * <p>Kits that require event handling should also implement {@link Listener} and add
 * {@code @EventHandler} methods — the {@link KitManager} will register them
 * automatically via {@link #getListeners()}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class KitJumper extends Kit {
 *     public KitJumper(Game game) {
 *         super(game, "Jumper", Material.FEATHER,
 *               new String[]{ "§7• §aDouble Jump", "§7• §aNo Fall Damage" },
 *               new PerkDoubleJump(0.65, 1.2),
 *               new PerkNoFallDamage());
 *     }
 * }
 * }</pre>
 */
public abstract class Kit {

    @Getter private final Game game;
    @Getter private final String name;
    @Getter private final Material icon;
    @Getter private final String[] description;
    @Getter private final Perk[] perks;

    protected Kit(Game game, String name, Material icon, String[] description, Perk... perks) {
        this.game = game;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.perks = perks;
        for (Perk perk : perks) {
            if (perk != null) perk.setKit(this);
        }
    }

    // =========================================================================
    // Core API
    // =========================================================================

    /**
     * Called when this kit is applied to a player (at game start).
     * Default behaviour: invoke every perk's {@link Perk#apply(Player)}, then
     * call {@link #giveItems(Player)}.  Override {@link #giveItems(Player)} to
     * add kit-specific starting items.
     */
    public void apply(Player player) {
        for (Perk perk : perks) {
            if (perk != null) perk.apply(player);
        }
        giveItems(player);
    }

    /**
     * Called when a player with this kit leaves the game or the game ends.
     * Cleans up perk effects (potion effects, flight flags, etc.).
     */
    public void remove(Player player) {
        for (Perk perk : perks) {
            if (perk != null) perk.remove(player);
        }
    }

    /**
     * Override to give the player this kit's starting items.
     * Called at the end of {@link #apply(Player)}.
     */
    protected void giveItems(Player player) {}

    /**
     * @return {@code true} if the player has selected this kit in the owning game.
     */
    public boolean hasKit(Player player) {
        return game.getKitManager().getKit(player) == this;
    }

    // =========================================================================
    // Listener collection
    // =========================================================================

    /**
     * Returns all {@link Listener} instances that belong to this kit (the kit itself
     * plus all of its perks that implement {@link Listener}).
     * The {@link KitManager} registers these with Bukkit when the game starts.
     */
    public List<Listener> getListeners() {
        List<Listener> listeners = new ArrayList<>();
        if (this instanceof Listener l) listeners.add(l);
        for (Perk perk : perks) {
            if (perk instanceof Listener l) listeners.add(l);
        }
        return listeners;
    }

    @Override
    public String toString() {
        return "Kit{" + name + ", perks=" + Arrays.stream(perks)
                .filter(p -> p != null).map(Perk::getName).toList() + "}";
    }
}

