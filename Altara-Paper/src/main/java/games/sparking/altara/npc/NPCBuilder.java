package games.sparking.altara.npc;

import games.sparking.altara.hologram.HologramProvider;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Fluent builder for {@link NPC}.
 *
 * <h2>Examples</h2>
 *
 * <pre>{@code
 * // Simple, static NPC
 * NPC npc = new NPCBuilder()
 *         .at(spawnLoc)
 *         .name("Greeter")
 *         .skin(preloadedSkin)
 *         .nametag("&6Greeter", "&7Click me!")
 *         .clickHandler((p, n, t) -> p.sendMessage("Hello!"))
 *         .build();
 * npc.spawn();
 *
 * // Per-player nametag (e.g. shows the viewer's rank)
 * NPC npc = new NPCBuilder()
 *         .at(selectorLoc)
 *         .name("Server NPC")
 *         .skin(skin)
 *         .nametagProvider(player -> List.of(
 *                 "&6Server Selector",
 *                 "&7Players: &f" + getOnline(),
 *                 "&7Your rank: " + getRank(player)
 *         ))
 *         .clickHandler((p, n, t) -> openMenu(p))
 *         .build();
 * npc.spawn();
 * }</pre>
 */
@Getter
public class NPCBuilder {

    private Location location;
    private String name;
    private NPCSkin skin;
    private HologramProvider nametagProvider;
    private double nametagYOffset  = 2.1;   // above player head (~1.8 block tall entity)
    private double nametagSpacing  = 0.25;
    private NPCClickHandler clickHandler;
    private Predicate<Player> visibilityFilter;

    // =========================================================================
    // Location & identity
    // =========================================================================

    public NPCBuilder at(Location location) {
        this.location = location;
        return this;
    }

    /** Display name used in the GameProfile (shown in tab-list briefly during spawn). */
    public NPCBuilder name(String name) {
        this.name = name;
        return this;
    }

    // =========================================================================
    // Skin
    // =========================================================================

    public NPCBuilder skin(NPCSkin skin) {
        this.skin = skin;
        return this;
    }

    // =========================================================================
    // Nametag – static convenience
    // =========================================================================

    /**
     * Sets fixed nametag lines identical for every viewer.
     * The first element is the top line; subsequent elements go downward.
     */
    public NPCBuilder nametag(String... lines) {
        List<String> list = List.copyOf(Arrays.asList(lines));
        this.nametagProvider = player -> list;
        return this;
    }

    /** Sets fixed nametag lines from a list. */
    public NPCBuilder nametag(List<String> lines) {
        List<String> copy = List.copyOf(lines);
        this.nametagProvider = player -> copy;
        return this;
    }

    // =========================================================================
    // Nametag – per-player provider
    // =========================================================================

    /**
     * Sets a per-player nametag provider.  The lambda is called once per update per viewer,
     * allowing each player to see personalised content.
     */
    public NPCBuilder nametagProvider(HologramProvider provider) {
        this.nametagProvider = provider;
        return this;
    }

    /**
     * Adjusts how high the top nametag line appears above the NPC's feet.
     * Default is {@code 2.1} (works well for player-height entities).
     */
    public NPCBuilder nametagOffset(double yOffset) {
        this.nametagYOffset = yOffset;
        return this;
    }

    /** Line spacing between nametag lines (default {@code 0.25}). */
    public NPCBuilder nametagSpacing(double spacing) {
        this.nametagSpacing = spacing;
        return this;
    }

    // =========================================================================
    // Click
    // =========================================================================

    public NPCBuilder clickHandler(NPCClickHandler handler) {
        this.clickHandler = handler;
        return this;
    }

    // =========================================================================
    // Visibility
    // =========================================================================

    /** Only players matching {@code filter} will see this NPC. */
    public NPCBuilder visibleTo(Predicate<Player> filter) {
        this.visibilityFilter = filter;
        return this;
    }

    /** Restricts visibility to a single specific player. */
    public NPCBuilder visibleTo(Player player) {
        this.visibilityFilter = p -> p.getUniqueId().equals(player.getUniqueId());
        return this;
    }

    // =========================================================================
    // Build
    // =========================================================================

    /**
     * Constructs and registers the {@link NPC}.  The NPC is <b>not</b> automatically
     * spawned — call {@link NPC#spawn()} or {@link NPC#spawn(Player)} yourself.
     */
    public NPC build() {
        return new NPC(this);
    }

    /** Constructs, registers, and immediately spawns for all online players. */
    public NPC buildAndSpawn() {
        NPC npc = build();
        npc.spawn();
        return npc;
    }
}