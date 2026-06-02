package games.sparking.altara.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base hologram.  Every line is rendered per-player via raw PacketEvents packets —
 * no real Bukkit entities are spawned.
 *
 * <p>Text lines use a {@code TEXT_DISPLAY} entity; click hitboxes use an
 * {@code INTERACTION} entity.  Both are purely client-side: they exist nowhere
 * on the server.
 *
 * <p>Click detection is handled by {@link games.sparking.altara.hologram.listener.HologramClickListener}
 * which listens for the {@code INTERACT_ENTITY} packet.
 */
@Getter
public abstract class Hologram {

    // -----------------------------------------------------------------------
    // Fake entity ID pool — counted DOWN from near Integer.MAX_VALUE so IDs
    // never collide with real server entities (which count up from 1).
    // -----------------------------------------------------------------------
    private static final AtomicInteger ENTITY_ID_POOL =
            new AtomicInteger(Integer.MAX_VALUE - 1_000);

    static int nextEntityId() {
        return ENTITY_ID_POOL.getAndDecrement();
    }

    // -----------------------------------------------------------------------
    // Global click-entity registry
    // Maps fake interaction entity ID → (hologram, lineIndex) so the click
    // listener can look up a hit without scanning every hologram.
    // -----------------------------------------------------------------------

    /** Data associated with a clickable interaction entity. */
    public record ClickData(Hologram hologram, int lineIndex) {}

    /** interactionEntityId → ClickData */
    private static final Map<Integer, ClickData> CLICK_MAP = new ConcurrentHashMap<>();

    public static ClickData getClickData(int entityId) {
        return CLICK_MAP.get(entityId);
    }

    // -----------------------------------------------------------------------
    // Per-player spawned line IDs
    // -----------------------------------------------------------------------

    /**
     * Pair of fake entity IDs for one rendered line.
     * {@code textId} is the TEXT_DISPLAY; {@code interactionId} is the INTERACTION hitbox.
     * Either may be -1 if not spawned (e.g. empty line has no text entity).
     */
    record LineIds(int textId, int interactionId) {}

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------

    private final int id;
    private Location location;
    private double lineSpacing;
    private final HologramClickHandler clickHandler;

    /** Empty = visible to all players.  Non-empty = only these UUIDs see the hologram. */
    private final Set<UUID> viewers = new LinkedHashSet<>();

    /** Per-player list of spawned line IDs (indexed parallel to getLines()). */
    private final Map<UUID, List<LineIds>> playerLines = new ConcurrentHashMap<>();

    protected Hologram(HologramBuilder builder) {
        if (builder.getLocation() == null)
            throw new IllegalArgumentException("Please provide a location using HologramBuilder#at");
        this.location     = builder.getLocation();
        this.lineSpacing  = builder.getLineSpacing();
        this.id           = HologramService.registerHologram(this);
        this.clickHandler = builder.getClickHandler();
        this.viewers.addAll(builder.getViewers());
    }

    /** Returns the current template lines (text only, no entity references). */
    public abstract List<HologramLine> getLines();

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /** Spawns for all currently applicable online players. */
    public void spawn() {
        if (viewers.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(this::spawnFor);
        } else {
            viewers.stream()
                   .map(Bukkit::getPlayer)
                   .filter(Objects::nonNull)
                   .forEach(this::spawnFor);
        }
    }

    /**
     * Sends fake entity packets for every line to {@code player}.
     * Any previously sent entities for this player are destroyed first.
     */
    public void spawnFor(Player player) {
        despawnFor(player.getUniqueId()); // clear stale state

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return; // PacketEvents not ready for this player yet — skip silently

        List<HologramLine> templates = getLines();
        // Top of the hologram is above the anchor; lines go downward.
        Location cursor = location.clone().add(0, templates.size() * lineSpacing, 0);
        List<LineIds> spawnedIds = new ArrayList<>(templates.size());

        for (int i = 0; i < templates.size(); i++) {
            String resolved = PlaceholderResolver.resolve(templates.get(i).getText(), player);
            Component component = HologramLine.toComponent(resolved);

            boolean hasText = resolved != null && !resolved.isEmpty();

            int textId = -1;
            if (hasText) {
                textId = nextEntityId();
                sendTextDisplay(user, textId, cursor, component);
            }

            int interactionId = nextEntityId();
            // Interaction hitbox sits at the cursor position (same as the text).
            sendInteraction(user, interactionId, cursor);
            CLICK_MAP.put(interactionId, new ClickData(this, i));

            spawnedIds.add(new LineIds(textId, interactionId));
            cursor.subtract(0, lineSpacing, 0);
        }

        playerLines.put(player.getUniqueId(), spawnedIds);
    }

    /** Destroys all fake entities for {@code player} and cleans up the click map. */
    public void despawnFor(Player player) {
        despawnFor(player.getUniqueId());
    }

    private void despawnFor(UUID uuid) {
        List<LineIds> lines = playerLines.remove(uuid);
        if (lines == null) return;

        // Collect all entity IDs to destroy in one packet.
        List<Integer> ids = new ArrayList<>(lines.size() * 2);
        for (LineIds line : lines) {
            if (line.textId() != -1) ids.add(line.textId());
            if (line.interactionId() != -1) {
                ids.add(line.interactionId());
                CLICK_MAP.remove(line.interactionId());
            }
        }
        if (ids.isEmpty()) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return; // PacketEvents already cleaned up — player disconnected, no packet needed
        user.sendPacket(new WrapperPlayServerDestroyEntities(ids.stream().mapToInt(Integer::intValue).toArray()));
    }

    /** Destroys fake entities for every player (does NOT unregister from HologramService). */
    public void destroy() {
        new ArrayList<>(playerLines.keySet()).forEach(this::despawnFor);
    }

    /**
     * Destroys entities AND removes this hologram from the global registry.
     * Call this when the hologram is permanently gone.
     */
    public void unregister() {
        destroy();
        HologramService.unregisterHologram(id);
    }

    /**
     * Refreshes rendered text for every current viewer.
     * If the line count changed the hologram is fully re-spawned for that player;
     * otherwise only the TEXT_DISPLAY metadata is updated.
     */
    public void update() {
        List<HologramLine> templates = getLines();

        for (UUID uuid : new ArrayList<>(playerLines.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) { despawnFor(uuid); continue; }

            List<LineIds> current = playerLines.get(uuid);
            if (current == null) continue;

            if (current.size() != templates.size()) {
                spawnFor(player);
                continue;
            }

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user == null) { despawnFor(uuid); continue; } // player disconnecting — clean up

            for (int i = 0; i < current.size(); i++) {
                int textId = current.get(i).textId();
                if (textId == -1) continue;
                String resolved = PlaceholderResolver.resolve(templates.get(i).getText(), player);
                sendTextMetadata(user, textId, HologramLine.toComponent(resolved));
            }
        }
    }

    /**
     * Updates the TEXT_DISPLAY metadata for each line in-place for {@code player},
     * without destroying or re-creating any entities (no flicker).
     *
     * <p>If the number of currently-spawned lines for {@code player} doesn't match
     * {@code newLines}, the method does nothing and returns {@code false} — the
     * caller should fall back to a full {@link #spawnFor} in that case.
     *
     * @param player   the viewer to update
     * @param newLines the new line content (resolved text, same length as current)
     * @return {@code true} if the update was applied, {@code false} if a respawn is needed
     */
    protected boolean updateTextInPlace(Player player, List<HologramLine> newLines) {
        List<LineIds> current = playerLines.get(player.getUniqueId());
        if (current == null || current.size() != newLines.size()) return false;

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return false;

        for (int i = 0; i < current.size(); i++) {
            int textId = current.get(i).textId();
            if (textId == -1) continue;
            String resolved = PlaceholderResolver.resolve(newLines.get(i).getText(), player);
            sendTextMetadata(user, textId, HologramLine.toComponent(resolved));
        }
        return true;
    }

    /** Moves the hologram and re-spawns it for every active viewer. */
    public void setLocation(Location location) {
        this.location = location;
        destroy();
        spawn();
    }

    /** Changes the line spacing and re-spawns the hologram for every active viewer. */
    public void setLineSpacing(double spacing) {
        this.lineSpacing = spacing;
        destroy();
        spawn();
    }

    // -----------------------------------------------------------------------
    // Viewer management
    // -----------------------------------------------------------------------

    /** Adds explicit viewers and immediately spawns entities for them. */
    public void addViewer(Player... players) {
        for (Player player : players) {
            viewers.add(player.getUniqueId());
            spawnFor(player);
        }
    }

    /** Removes a viewer and destroys their entities. */
    public void removeViewer(Player player) {
        viewers.remove(player.getUniqueId());
        despawnFor(player.getUniqueId());
    }

    // -----------------------------------------------------------------------
    // Click detection helpers (used by HologramClickListener)
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if hologram entities have been spawned for {@code player}
     * and are still tracked (i.e. the player is an active viewer).
     */
    public boolean isSpawnedFor(Player player) {
        return playerLines.containsKey(player.getUniqueId());
    }

    /**
     * Returns the 0-based line index whose INTERACTION entity matches
     * {@code entityId} for this specific player, or -1 if not found.
     */
    public int getClickedLineIndexByEntityId(Player player, int entityId) {
        List<LineIds> lines = playerLines.get(player.getUniqueId());
        if (lines == null) return -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).interactionId() == entityId) return i;
        }
        return -1;
    }

    // -----------------------------------------------------------------------
    // Packet helpers
    // -----------------------------------------------------------------------

    /** Sends a TEXT_DISPLAY spawn + metadata to {@code user}. */
    private static void sendTextDisplay(User user, int entityId, Location loc, Component text) {
        UUID uuid = UUID.randomUUID();
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), EntityTypes.TEXT_DISPLAY,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                0f, 0f, 0f, 0, Optional.empty());
        user.sendPacket(spawn);

        sendTextMetadata(user, entityId, text);
    }

    /** Sends (or re-sends) the TEXT_DISPLAY metadata — used both on spawn and on update. */
    private static void sendTextMetadata(User user, int entityId, Component text) {
        List<EntityData<?>> meta = new ArrayList<>();
        // Billboard: CENTER (3) — text always faces the player.
        meta.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) 3));
        // Text content.
        meta.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, text));
        // Line width before wrapping
        meta.add(new EntityData<>(24, EntityDataTypes.INT, 500));
        // Transparent background (ARGB 0 = fully transparent).
//        meta.add(new EntityData<>(25, EntityDataTypes.INT, 0));
        user.sendPacket(new WrapperPlayServerEntityMetadata(entityId, meta));
    }

    /** Sends an INTERACTION entity spawn + metadata to {@code user}. */
    private static void sendInteraction(User user, int entityId, Location loc) {
        UUID uuid = UUID.randomUUID();
        // Slight Y offset so the hitbox is centered on the text line.
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), EntityTypes.INTERACTION,
                new Vector3d(loc.getX(), loc.getY() - 0.125, loc.getZ()),
                0f, 0f, 0f, 0, Optional.empty());
        user.sendPacket(spawn);

        List<EntityData<?>> meta = new ArrayList<>();
        meta.add(new EntityData<>(8, EntityDataTypes.FLOAT, 0.8f));   // width
        meta.add(new EntityData<>(9, EntityDataTypes.FLOAT, 0.25f));  // height
        user.sendPacket(new WrapperPlayServerEntityMetadata(entityId, meta));
    }
}
