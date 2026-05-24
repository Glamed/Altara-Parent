package games.sparking.altara.hologram;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.meta.other.ArmorStandMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A hologram built from one or more invisible {@link WrapperEntity} armor stands that display
 * floating text lines.  The same class handles both static and auto-updating holograms:
 *
 * <ul>
 *   <li><b>Static</b> – pass {@link HologramProvider#fixed(String...)} as the provider and
 *       leave the update interval at 0.</li>
 *   <li><b>Updating</b> – supply a dynamic provider and call
 *       {@link HologramBuilder#updateInterval(long)} before building.</li>
 * </ul>
 *
 * <p>Holograms are <em>per-player</em>: each online player gets their own set of armor-stand
 * entities so that the provider can return personalised lines.  Armor stands keep their normal
 * hitbox (not Markers) so that {@link HologramClickHandler} works out of the box.
 *
 * <p>All registered holograms are managed by {@link HologramService}, which auto-spawns /
 * despawns them as players join or leave.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Hologram holo = new HologramBuilder()
 *         .at(someLocation)
 *         .provider(player -> List.of("&6Hello " + player.getName()))
 *         .updateIntervalTicks(40)       // refresh every 2 s (optional)
 *         .clickHandler((p, h, type) -> p.sendMessage("Clicked!"))
 *         .build();
 *
 * holo.spawn(); // show to all online players
 * }</pre>
 */
@Getter
public class Hologram {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    // =========================================================================
    // Configurable fields
    // =========================================================================

    @Setter private Location location;
    @Setter private double lineSpacing;
    @Setter private HologramClickHandler clickHandler;
    /** Optional filter – returning {@code false} hides this hologram from that player. */
    @Setter private Predicate<Player> visibilityFilter;

    private final HologramProvider provider;
    private final long updateIntervalTicks; // 0 = no auto-update task

    // =========================================================================
    // Runtime state (not exposed publicly)
    // =========================================================================

    /** Per-player armor-stand entities: UUID → line entities (top→bottom). */
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, List<WrapperEntity>> playerEntities = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private BukkitTask updateTask;

    // =========================================================================
    // Constructor (package-private – use HologramBuilder)
    // =========================================================================

    protected Hologram(HologramBuilder builder) {
        if (builder.getLocation() == null)
            throw new IllegalArgumentException("Hologram location must be set via HologramBuilder#at(Location)");
        if (builder.getProvider() == null)
            throw new IllegalArgumentException("A HologramProvider must be set via HologramBuilder#provider(...)");

        this.location            = builder.getLocation();
        this.lineSpacing         = builder.getLineSpacing();
        this.provider            = builder.getProvider();
        this.clickHandler        = builder.getClickHandler();
        this.updateIntervalTicks = builder.getUpdateIntervalTicks();
        this.visibilityFilter    = builder.getVisibilityFilter();

        HologramService.register(this);
    }

    // =========================================================================
    // Spawn / Despawn
    // =========================================================================

    /** Spawns this hologram for every currently online player that passes the visibility filter. */
    public void spawn() {
        Bukkit.getOnlinePlayers().forEach(this::spawn);
    }

    /**
     * Spawns this hologram for {@code player} if they pass the visibility filter and it has not
     * already been spawned for them.
     */
    public void spawn(Player player) {
        if (!canSee(player)) return;
        if (playerEntities.containsKey(player.getUniqueId())) return;

        List<String> lines = provider.getLines(player);
        List<WrapperEntity> entities = new ArrayList<>(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            WrapperEntity entity = createLineEntity(lines.get(i));
            entity.addViewer(player.getUniqueId());
            entity.spawn(lineLocation(i));
            entities.add(entity);
            HologramService.registerEntityId(entity.getEntityId(), this);
        }

        playerEntities.put(player.getUniqueId(), entities);
    }

    /** Removes this hologram from every player's view and cleans up all entities. */
    public void despawn() {
        new ArrayList<>(playerEntities.keySet()).forEach(uuid -> {
            List<WrapperEntity> entities = playerEntities.remove(uuid);
            if (entities != null) cleanupEntities(entities);
        });
    }

    /**
     * Removes this hologram from {@code player}'s view and despawns their entities.
     * Safe to call even if the hologram was not spawned for them.
     */
    public void despawn(Player player) {
        List<WrapperEntity> entities = playerEntities.remove(player.getUniqueId());
        if (entities != null) cleanupEntities(entities);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Refreshes line text for every online player.  If the number of lines changes for a player
     * the entities are re-created; otherwise only the name-tag packets are sent.
     */
    public void update() {
        Bukkit.getOnlinePlayers().forEach(this::update);
    }

    /**
     * Refreshes line text for {@code player}.
     * <ul>
     *   <li>If they can no longer see the hologram, it is despawned for them.</li>
     *   <li>If it was not yet spawned, it is spawned now.</li>
     *   <li>If the line count changed, entities are recreated.</li>
     *   <li>Otherwise only the custom-name metadata is pushed (lightweight).</li>
     * </ul>
     */
    public void update(Player player) {
        if (!canSee(player)) {
            despawn(player);
            return;
        }

        List<WrapperEntity> existing = playerEntities.get(player.getUniqueId());

        if (existing == null) {
            spawn(player);
            return;
        }

        List<String> newLines = provider.getLines(player);

        if (existing.size() != newLines.size()) {
            // Recreate entities to match new line count
            despawn(player);
            spawn(player);
            return;
        }

        // Fast path – just update names
        for (int i = 0; i < newLines.size(); i++) {
            ArmorStandMeta meta = (ArmorStandMeta) existing.get(i).getEntityMeta();
            Component text = fromLegacy(newLines.get(i));
            meta.setCustomName(text);
            meta.setCustomNameVisible(!newLines.get(i).isBlank());
        }
    }

    // =========================================================================
    // Update task management
    // =========================================================================

    /**
     * Starts the async update task if an update interval was configured.
     * Does nothing if already running or if no interval is set.
     */
    public void startUpdateTask() {
        if (updateIntervalTicks <= 0 || updateTask != null) return;
        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                AltaraPaper.getPlugin(), this::update, 0L, updateIntervalTicks);
    }

    /** Stops the auto-update task. */
    public void cancelUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    // =========================================================================
    // Location helpers
    // =========================================================================

    /**
     * Teleports the hologram to {@code newLocation}, despawning and respawning all entities so
     * the move is applied immediately for all viewers.
     */
    public void moveTo(Location newLocation) {
        despawn();
        this.location = newLocation;
        spawn();
    }

    // =========================================================================
    // Visibility
    // =========================================================================

    /** Returns {@code true} if {@code player} should see this hologram. */
    public boolean canSee(Player player) {
        if (visibilityFilter != null && !visibilityFilter.test(player)) return false;
        return player.getWorld().equals(location.getWorld());
    }

    /** Returns {@code true} if this hologram has been spawned (and not yet despawned) for {@code player}. */
    public boolean isSpawnedFor(Player player) {
        return playerEntities.containsKey(player.getUniqueId());
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /** The world location where line {@code index} (0 = top) should be placed. */
    private Location lineLocation(int lineIndex) {
        return location.clone().subtract(0, lineIndex * lineSpacing, 0);
    }

    /**
     * Creates an invisible armor-stand entity with the given line text.
     * The entity is NOT spawned or shown to any viewer yet; callers are responsible for that.
     *
     * <p>The armor stand is <em>not</em> a Marker (so it retains a hitbox for click detection),
     * but its body is invisible.  Only the custom name tag (the hologram text) is visible.
     */
    private WrapperEntity createLineEntity(String text) {
        WrapperEntity entity = EntityLib.getApi().createEntity(
                UUID.randomUUID(), EntityTypes.ARMOR_STAND);

        ArmorStandMeta meta = (ArmorStandMeta) entity.getEntityMeta();
        meta.setHasNoGravity(true);
        meta.setInvisible(true);      // invisible body
        meta.setSmall(false);
        meta.setHasArms(false);
        meta.setHasBasePlate(false);
        // NOTE: setMarker(true) is intentionally NOT set so that the hitbox is preserved
        // for click detection via HologramClickHandler.
        meta.setCustomName(fromLegacy(text));
        meta.setCustomNameVisible(!text.isBlank());

        return entity;
    }

    /** Despawns and removes a list of entities from the EntityLib registry + HologramService. */
    private static void cleanupEntities(List<WrapperEntity> entities) {
        for (WrapperEntity entity : entities) {
            HologramService.unregisterEntityId(entity.getEntityId());
            entity.despawn();
            EntityLib.getApi().removeEntity(entity);
        }
    }

    /** Converts a legacy-formatted string (using {@code &} or {@code §} codes) to a Component. */
    private static Component fromLegacy(String text) {
        // Normalise & → § before deserialising
        return LEGACY.deserialize(text.replace('&', '§'));
    }
}
