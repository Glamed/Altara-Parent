package games.sparking.altara.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.HologramProvider;
import games.sparking.altara.hologram.HologramService;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A virtual player-skin NPC powered by PacketEvents' built-in NPC implementation,
 * augmented with an optional multi-line <em>nametag hologram</em> built on the
 * {@link Hologram} system.
 *
 * <p>The body is spawned per-player through the PacketEvents channel system.
 * Adding a nametag creates a {@link Hologram} whose visibility is gated to players
 * for whom the NPC body is currently showing, so the nametag tracks the NPC exactly.
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
    // Runtime – nametag hologram
    // =========================================================================

    /**
     * The nametag {@link Hologram} for this NPC, or {@code null} if no nametag was configured.
     * Its visibility filter is wired to {@link #spawnedFor} so it only shows for players
     * that already see the NPC body.
     */
    @Getter(AccessLevel.NONE)
    private final Hologram nametag;

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

        // Build the nametag hologram (only when a provider was supplied).
        // Visibility is gated to players that currently see the NPC body.
        if (nametagProvider != null) {
            Location nametagBase = location.clone().add(0, nametagYOffset, 0);
            this.nametag = new HologramBuilder()
                    .at(nametagBase)
                    .withSpacing(nametagSpacing)
                    .provider(nametagProvider)
                    .visibleTo(player -> spawnedFor.contains(player.getUniqueId()))
                    .clickHandler((player, ignored, type) -> {
                        if (clickHandler != null)
                            clickHandler.click(player, this, NPCClickHandler.ClickType.valueOf(type.name()));
                    })
                    .build();
        } else {
            this.nametag = null;
        }

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
     *   <li>Spawns the nametag hologram for this player (if a provider is configured).</li>
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

        if (nametag != null) nametag.spawn(player);
    }

    /** Removes the NPC from every player's view and releases the nametag hologram. */
    public void despawn() {
        new ArrayList<>(spawnedFor).forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                body.despawn(PacketEvents.getAPI().getPlayerManager().getChannel(player));
            }
            spawnedFor.remove(uuid);
        });
        if (nametag != null) HologramService.unregister(nametag); // despawns for all + removes from registry
        NPCService.unregisterEntityId(entityId);
    }

    /** Removes the NPC from {@code player}'s view. Safe to call even if not currently shown. */
    public void despawn(Player player) {
        if (!spawnedFor.remove(player.getUniqueId())) return;
        body.despawn(PacketEvents.getAPI().getPlayerManager().getChannel(player));
        if (nametag != null) nametag.despawn(player);
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
        if (nametag == null) return;
        if (!spawnedFor.contains(player.getUniqueId())) {
            spawn(player);
            return;
        }
        nametag.update(player);
    }

    // =========================================================================
    // Location
    // =========================================================================

    /**
     * Teleports the NPC to a new location.  All current viewers receive the teleport packet;
     * the nametag hologram is moved to match.
     */
    public void moveTo(Location newLocation) {
        this.location = newLocation;
        for (UUID uuid : spawnedFor) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            body.teleport(toPELocation(newLocation));
            body.updateRotation(newLocation.getYaw(), newLocation.getPitch());
        }
        if (nametag != null) {
            nametag.moveTo(newLocation.clone().add(0, nametagYOffset, 0));
        }
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
        PacketEvents.getAPI().getPlayerManager().getUser(player).sendPacket(meta.createPacket());
    }

    /**
     * Sends scoreboard-team packets to {@code player} that set {@link WrapperPlayServerTeams.NameTagVisibility#NEVER}
     * for this NPC, hiding the default floating name label so only the custom hologram nametag is visible.
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


    private static com.github.retrooper.packetevents.protocol.world.Location toPELocation(Location loc) {
        return new com.github.retrooper.packetevents.protocol.world.Location(
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

}

