package games.sparking.altara.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import games.sparking.altara.hologram.HologramProvider;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.other.ArmorStandMeta;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A virtual player-skin NPC powered by PacketEvents' built-in NPC implementation,
 * augmented with an optional multi-line <em>nametag hologram</em> made from EntityLib
 * armor-stand entities.
 *
 * <p>The body is spawned per-player through the PacketEvents channel system.
 * Adding a nametag creates per-player invisible armor stands that float above the NPC
 * body — the same technique used by {@link games.sparking.altara.hologram.Hologram} —
 * allowing each viewer to see different personalised text (rank, live stats, etc.).
 *
 * <p>All instances register themselves automatically with {@link NPCService}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * NPCSkin.fetchAsync("Notch", plugin, skin -> {
 *     new NPCBuilder()
 *             .at(location)
 *             .name("Server Selector")
 *             .skin(skin)
 *             .nametag("&6&lServer Selector", "&7Right-click to browse!")
 *             .clickHandler((player, npc, type) -> new ServerSelectorMenu(player).open())
 *             .buildAndSpawn();
 * });
 * }</pre>
 */
@Getter
public final class NPC {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    // =========================================================================
    // Config
    // =========================================================================

    @Setter private Location location;
    @Setter private NPCClickHandler clickHandler;
    @Setter private Predicate<Player> visibilityFilter;

    private final String name;
    private final List<TextureProperty> skinProperties; // empty = no skin / default player
    private final HologramProvider nametagProvider;     // null = no hologram nametag
    private final double nametagYOffset;
    private final double nametagSpacing;

    // =========================================================================
    // Runtime – body
    // =========================================================================

    /**
     * The underlying PacketEvents NPC entity.
     * Stored with FQN to avoid a naming conflict with this class.
     */
    @Getter(AccessLevel.NONE)
    private final com.github.retrooper.packetevents.protocol.npc.NPC body;

    /** Entity ID used for O(1) click routing in {@link NPCService}. */
    private final int entityId;

    /** Players for whom the body is currently visible. */
    @Getter(AccessLevel.NONE)
    private final Set<UUID> spawnedFor = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // =========================================================================
    // Runtime – nametag
    // =========================================================================

    /**
     * Per-player nametag armor stands (top to bottom order).
     * Only populated when {@link #nametagProvider} is non-null.
     */
    @Getter(AccessLevel.NONE)
    private final Map<UUID, List<WrapperEntity>> nametagEntities = new ConcurrentHashMap<>();

    // =========================================================================
    // Constructor (package-private – use NPCBuilder)
    // =========================================================================

    NPC(NPCBuilder builder) {
        if (builder.getLocation() == null)
            throw new IllegalArgumentException("NPC location must be set via NPCBuilder#at(Location)");

        this.location         = builder.getLocation();
        this.name             = builder.getName() != null ? builder.getName() : "NPC";
        this.skinProperties   = builder.getSkin() != null ? builder.getSkin().toTextureProperties() : List.of();
        this.nametagProvider  = builder.getNametagProvider();
        this.nametagYOffset   = builder.getNametagYOffset();
        this.nametagSpacing   = builder.getNametagSpacing();
        this.clickHandler     = builder.getClickHandler();
        this.visibilityFilter = builder.getVisibilityFilter();

        this.entityId = SpigotReflectionUtil.generateEntityId();
        this.body     = new com.github.retrooper.packetevents.protocol.npc.NPC(
                new UserProfile(UUID.randomUUID(), this.name, this.skinProperties),
                this.entityId,
                GameMode.SURVIVAL,
                null, null, null, null);

        NPCService.register(this);
        NPCService.registerEntityId(entityId, this);
    }

    // =========================================================================
    // Spawn / Despawn
    // =========================================================================

    /** Shows this NPC to all currently online players that pass the visibility filter. */
    public void spawn() {
        Bukkit.getOnlinePlayers().forEach(this::spawn);
    }

    /**
     * Shows the NPC to {@code player}.
     * <ol>
     *   <li>Positions and spawns the body on the player's PacketEvents channel.</li>
     *   <li>Sends PlayerMeta (all skin layers enabled).</li>
     *   <li>Sends team packets so the default floating player-name label is hidden.</li>
     *   <li>Spawns per-player nametag armor stands (if a provider is configured).</li>
     * </ol>
     */
    public void spawn(Player player) {
        if (!canSee(player)) return;
        if (spawnedFor.contains(player.getUniqueId())) return;
        spawnedFor.add(player.getUniqueId());

        body.teleport(toPELocation(location));
        body.updateRotation(location.getYaw(), location.getPitch());
        body.spawn(PacketEvents.getAPI().getPlayerManager().getChannel(player));

        sendSkinMeta(player);
        sendHideNametag(player);

        if (nametagProvider != null) spawnNametag(player);
    }

    /** Removes the NPC from every player's view and releases nametag entities. */
    public void despawn() {
        new ArrayList<>(spawnedFor).forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                despawn(player);
            } else {
                spawnedFor.remove(uuid);
                List<WrapperEntity> lines = nametagEntities.remove(uuid);
                if (lines != null) cleanupEntities(lines);
            }
        });
        NPCService.unregisterEntityId(entityId);
    }

    /** Removes the NPC from {@code player}'s view. Safe to call even if not currently shown. */
    public void despawn(Player player) {
        if (!spawnedFor.remove(player.getUniqueId())) return;
        body.despawn(PacketEvents.getAPI().getPlayerManager().getChannel(player));
        List<WrapperEntity> lines = nametagEntities.remove(player.getUniqueId());
        if (lines != null) cleanupEntities(lines);
    }

    // =========================================================================
    // Update (nametag)
    // =========================================================================

    /** Refreshes the nametag text for every current viewer.  No-op if no nametag provider. */
    public void update() {
        Bukkit.getOnlinePlayers().forEach(this::update);
    }

    /** Refreshes the nametag for {@code player}. */
    public void update(Player player) {
        if (nametagProvider == null) return;
        if (!spawnedFor.contains(player.getUniqueId())) {
            spawn(player);
            return;
        }

        List<WrapperEntity> existing = nametagEntities.get(player.getUniqueId());
        List<String> newLines = nametagProvider.getLines(player);

        if (existing == null) {
            spawnNametag(player);
            return;
        }

        if (existing.size() != newLines.size()) {
            cleanupEntities(nametagEntities.remove(player.getUniqueId()));
            spawnNametag(player);
            return;
        }

        // Fast path – update custom names in place without respawning entities
        for (int i = 0; i < newLines.size(); i++) {
            ArmorStandMeta meta = (ArmorStandMeta) existing.get(i).getEntityMeta();
            meta.setCustomName(fromLegacy(newLines.get(i)));
            meta.setCustomNameVisible(!newLines.get(i).isBlank());
        }
    }

    // =========================================================================
    // Location
    // =========================================================================

    /**
     * Teleports the NPC to a new location.  All current viewers receive the teleport packet;
     * nametag entities are destroyed and recreated at the new position.
     */
    public void moveTo(Location newLocation) {
        this.location = newLocation;
        for (UUID uuid : spawnedFor) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            body.teleport(toPELocation(newLocation));
            body.updateRotation(newLocation.getYaw(), newLocation.getPitch());
        }
        new ArrayList<>(nametagEntities.keySet()).forEach(uuid -> {
            List<WrapperEntity> lines = nametagEntities.remove(uuid);
            if (lines != null) cleanupEntities(lines);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) spawnNametag(player);
        });
    }

    // =========================================================================
    // Rotation
    // =========================================================================

    /** Rotates the NPC body to the given yaw for all current viewers. */
    public void rotate(float yaw) {
        body.updateRotation(yaw, 0);
    }

    // =========================================================================
    // Visibility
    // =========================================================================

    /** Returns {@code true} if {@code player} passes the visibility filter and world check. */
    public boolean canSee(Player player) {
        if (visibilityFilter != null && !visibilityFilter.test(player)) return false;
        return player.getWorld().equals(location.getWorld());
    }

    /** Returns {@code true} if the NPC is currently spawned for {@code player}. */
    public boolean isSpawnedFor(Player player) {
        return spawnedFor.contains(player.getUniqueId());
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /** Sends a PlayerMeta packet that enables all skin layers (jacket, sleeves, hat, etc.). */
    private void sendSkinMeta(Player player) {
        EntityMeta meta = EntityMeta.createMeta(entityId, EntityTypes.PLAYER);
        PlayerMeta playerMeta = (PlayerMeta) meta;
        playerMeta.setJacketEnabled(true);
        playerMeta.setRightLegEnabled(true);
        playerMeta.setLeftLegEnabled(true);
        playerMeta.setRightSleeveEnabled(true);
        playerMeta.setLeftSleeveEnabled(true);
        playerMeta.setHatEnabled(true);
        playerMeta.setCapeEnabled(true);
        WrapperPlayServerEntityMetadata metaPacket = meta.createPacket();
        PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(metaPacket);
    }

    /**
     * Sends scoreboard-team packets to {@code player} that set {@link WrapperPlayServerTeams.NameTagVisibility#NEVER}
     * for this NPC, hiding the default floating name label so only the custom armor-stand
     * nametag hologram is visible.
     */
    private void sendHideNametag(Player player) {
        String teamName = "anpc_" + Integer.toHexString(entityId);
        List<String> members = List.of(
                body.getProfile().getName(),
                String.valueOf(entityId),
                body.getProfile().getUUID().toString());

        WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(), Component.empty(), Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                NamedTextColor.WHITE,
                WrapperPlayServerTeams.OptionData.NONE);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerTeams(teamName, WrapperPlayServerTeams.TeamMode.REMOVE,
                        (WrapperPlayServerTeams.ScoreBoardTeamInfo) null, List.of()));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerTeams(teamName, WrapperPlayServerTeams.TeamMode.CREATE, teamInfo, members));
    }

    /** Spawns and registers per-player nametag armor stands for the given player. */
    private void spawnNametag(Player player) {
        List<String> lines = nametagProvider.getLines(player);
        List<WrapperEntity> entities = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            WrapperEntity line = createNametagLine(lines.get(i));
            line.addViewer(player.getUniqueId());
            line.spawn(toPELocation(nametagLocation(i)));
            entities.add(line);
            NPCService.registerEntityId(line.getEntityId(), this);
        }
        nametagEntities.put(player.getUniqueId(), entities);
    }

    /**
     * Creates a single invisible marker armor stand for one nametag line.
     * {@code setMarker(true)} gives it zero hitbox so all clicks pass through to the NPC body.
     */
    private WrapperEntity createNametagLine(String text) {
        WrapperEntity entity = new WrapperEntity(UUID.randomUUID(), EntityTypes.ARMOR_STAND);
        ArmorStandMeta meta = (ArmorStandMeta) entity.getEntityMeta();
        meta.setHasNoGravity(true);
        meta.setInvisible(true);
        meta.setSmall(false);
        meta.setHasArms(false);
        meta.setHasNoBasePlate(true);
        meta.setMarker(true);
        meta.setCustomName(fromLegacy(text));
        meta.setCustomNameVisible(!text.isBlank());
        return entity;
    }

    /** Computes the world position for nametag line {@code index} (0 = topmost). */
    private Location nametagLocation(int index) {
        return location.clone().add(0, nametagYOffset - index * nametagSpacing, 0);
    }

    private static void cleanupEntities(List<WrapperEntity> entities) {
        for (WrapperEntity e : entities) {
            NPCService.unregisterEntityId(e.getEntityId());
            e.despawn();
            e.remove();
        }
    }

    private static com.github.retrooper.packetevents.protocol.world.Location toPELocation(Location loc) {
        return new com.github.retrooper.packetevents.protocol.world.Location(
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    private static Component fromLegacy(String text) {
        return LEGACY.deserialize(text.replace('&', '§'));
    }
}



