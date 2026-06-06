package games.sparking.altara.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.*;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.npc.clickhandler.NPCClickHandler;
import games.sparking.altara.npc.config.NPCConfigEntry;
import games.sparking.altara.npc.equipment.EquipmentSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A packet-only NPC — no real server entity is ever spawned.
 *
 * <p>Packets are sent to each player client individually using PacketEvents.
 * Click detection is handled by
 * {@link games.sparking.altara.npc.listener.NPCClickListener}.
 *
 * <p>The nametag hologram is created automatically from the NPC's display name.
 */
@Getter
@Setter
public class NPC {

    // -----------------------------------------------------------------------
    // Fake entity ID pool
    // -----------------------------------------------------------------------
    private static final AtomicInteger ENTITY_ID_POOL =
            new AtomicInteger(Integer.MAX_VALUE - 2_000_000);

    static int nextEntityId() { return ENTITY_ID_POOL.getAndDecrement(); }

    // -----------------------------------------------------------------------
    // Global click registry  entityId → NPC
    // -----------------------------------------------------------------------
    private static final Map<Integer, NPC> ENTITY_CLICK_MAP = new ConcurrentHashMap<>();

    public static NPC getByEntityId(int entityId) {
        return ENTITY_CLICK_MAP.get(entityId);
    }

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------

    private final int  id;
    private final int  entityId;
    private final UUID uuid;

    /** Current nametag hologram, or {@code null} if display name is empty. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private @Nullable Hologram nametag;

    private String name;
    private String displayName = "";

    private Location location;
    private String   command;
    private boolean  consoleCommand;
    private String[] skin;
    private NPCClickHandler clickHandler = NPCClickHandler.COMMAND;

    @Setter(AccessLevel.NONE)
    private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    protected NPC(NPCBuilder builder) {
        if (builder.getLocation() == null)
            throw new IllegalArgumentException("Please provide a location using NPCBuilder#at");

        this.uuid     = UUID.randomUUID();
        this.entityId = nextEntityId();
        this.location = builder.getLocation();

        if (builder.getDisplayName() != null) this.displayName  = builder.getDisplayName();
        if (builder.getClickHandler() != null) this.clickHandler = builder.getClickHandler();
        if (builder.getCommand()      != null) this.command      = builder.getCommand();
        this.consoleCommand    = builder.isConsoleCommand();

        if (builder.getEquipment() != null)
            this.equipment.putAll(builder.getEquipment());

        this.id = NPCService.registerNpc(this);
        ENTITY_CLICK_MAP.put(entityId, this);

        // Create initial nametag from display name.
        this.nametag = buildNametag();

        if (builder.getSkinName() != null) {
            Bukkit.getScheduler().runTaskAsynchronously(AltaraPaper.getPlugin(), () -> {
                this.skin = fetchSkin(builder.getSkinName());
                Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(), this::spawn);
            });
        } else {
            this.skin = builder.getSkin();
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void spawn() {
        Bukkit.getOnlinePlayers().forEach(this::spawnFor);
    }

    public void spawnFor(Player player) {
        despawnFor(player);

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            Bukkit.getLogger().warning("[NPC] spawnFor: PacketEvents User is null for " + player.getName() + " — skipping NPC " + entityId);
            return;
        }


        // 1. Tab list entry (required before spawn in 1.20.2+).
        user.sendPacket(buildTabAddPacket());

        // 2. Spawn player entity.
        user.sendPacket(new WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), EntityTypes.PLAYER,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(), location.getYaw(), location.getYaw(),
                0, Optional.empty()));

        // 2a. Enable all skin layers (index 16 = Displayed Skin Parts on Avatar).
        user.sendPacket(new WrapperPlayServerEntityMetadata(entityId,
                List.of(new EntityData<>(16, EntityDataTypes.BYTE, (byte) 0x7f))));


        // 3. Head yaw.
        user.sendPacket(new WrapperPlayServerEntityHeadLook(entityId, location.getYaw()));

        // 4. Body rotation.
        user.sendPacket(new WrapperPlayServerEntityRotation(
                entityId, location.getYaw(), location.getPitch(), false));

        // 5. Equipment.
        List<Equipment> equipList = buildEquipmentList();
        if (!equipList.isEmpty())
            user.sendPacket(new WrapperPlayServerEntityEquipment(entityId, equipList));

        // 6. Nametag hologram (if display name is set).
        if (nametag != null) nametag.spawnFor(player);

        // 7. Scoreboard team to hide the vanilla nametag.
        try {
            sendNametagTeam(user);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NPC] sendNametagTeam threw: " + e.getMessage());
        }

        // 8. Hide from tab list after a short delay.
        Bukkit.getScheduler().runTaskLater(AltaraPaper.getPlugin(), () -> {
            if (!player.isOnline()) return;
            UserProfile hiddenProfile = new UserProfile(uuid, getProfileName());
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo hide =
                    new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                            hiddenProfile, false, 0, GameMode.SURVIVAL, null, null);
            user.sendPacket(new WrapperPlayServerPlayerInfoUpdate(
                    EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                    Collections.singletonList(hide)));
        }, 5L);

        viewers.add(player.getUniqueId());
    }

    public void despawnFor(Player player) {
        if (!viewers.remove(player.getUniqueId())) return;
        if (!player.isOnline()) return;

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;

        user.sendPacket(new WrapperPlayServerDestroyEntities(entityId));
        user.sendPacket(new WrapperPlayServerPlayerInfoRemove(Collections.singletonList(uuid)));
        if (nametag != null) nametag.despawnFor(player);
    }

    public void destroy() {
        List<UUID> copy = new ArrayList<>(viewers);
        for (UUID uid : copy) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) despawnFor(p);
            else viewers.remove(uid);
        }
    }

    public void unregister() {
        destroy();
        if (nametag != null) { nametag.unregister(); nametag = null; }
        ENTITY_CLICK_MAP.remove(entityId);
        NPCService.unregisterNpc(uuid);
    }

    // -----------------------------------------------------------------------
    // Rotation
    // -----------------------------------------------------------------------

    public void rotate(float yaw) {
        viewers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
               .forEach(p -> rotate(yaw, p));
    }

    public void rotate(float yaw, Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;
        user.sendPacket(new WrapperPlayServerEntityHeadLook(entityId, yaw));
        user.sendPacket(new WrapperPlayServerEntityRotation(entityId, yaw, location.getPitch(), false));
    }

    // -----------------------------------------------------------------------
    // Equipment
    // -----------------------------------------------------------------------

    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        if (item == null) equipment.remove(slot);
        else              equipment.put(slot, item);

        if (viewers.isEmpty()) return;
        List<Equipment> eq = Collections.singletonList(toEquipment(slot, item));
        viewers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
            User u = PacketEvents.getAPI().getPlayerManager().getUser(p);
            if (u != null) u.sendPacket(new WrapperPlayServerEntityEquipment(entityId, eq));
        });
    }

    // -----------------------------------------------------------------------
    // Property setters
    // -----------------------------------------------------------------------

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        // Recreate the nametag via the provider (it may depend on displayName).
        refreshNametag();
    }

    public void setSkin(String[] skin) {
        if (skin == null)
            Bukkit.getLogger().warning("[NPC] setSkin: null skin — will use default");
        this.skin = skin;
        respawn();
    }

    public void setLocation(Location location) {
        this.location = location;
        respawn();
    }

    /**
     * Destroys and re-spawns this NPC (and its nametag) on the main thread.
     * Safe to call from async contexts.
     */
    private void respawn() {
        Runnable task = () -> {
            destroy();
            // Recreate nametag at updated location.
            if (nametag != null) { nametag.unregister(); nametag = null; }
            nametag = buildNametag();
            spawn();
        };
        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(), task);
    }

    /**
     * Replaces the nametag hologram for currently viewing players without
     * doing a full NPC respawn.  Used by {@link #setDisplayName}.
     */
    private void refreshNametag() {
        // Capture current viewers before despawning.
        List<Player> current = viewers.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (nametag != null) {
            current.forEach(nametag::despawnFor);
            nametag.unregister();
            nametag = null;
        }
        nametag = buildNametag();
        if (nametag != null) current.forEach(nametag::spawnFor);
    }

    /** Builds the nametag hologram from the current display name, or returns {@code null}. */
    private @Nullable Hologram buildNametag() {
        if (displayName == null || displayName.isEmpty()) return null;
        var builder = new HologramBuilder()
                .at(nametagLocation())
                .staticHologram();
        for (String line : splitDisplayName(displayName))
            builder.addLines(toNametag(line));
        return builder.build();
    }

    // -----------------------------------------------------------------------
    // Visibility
    // -----------------------------------------------------------------------

    public boolean isSpawnedFor(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    // -----------------------------------------------------------------------
    // Config serialisation
    // -----------------------------------------------------------------------

    public NPCConfigEntry toConfig() {
        NPCConfigEntry entry = new NPCConfigEntry();
        entry.setLocation(new LocationConfig(location));
        entry.setName(name);
        entry.setDisplayName(displayName);
        if (command != null) { entry.setCommand(command); entry.setConsoleCommand(consoleCommand); }
        if (skin != null)    { entry.setTexture(skin[0]); entry.setSignature(skin[1]); }
        entry.setEquipment(new HashMap<>(equipment));
        return entry;
    }

    // -----------------------------------------------------------------------
    // Packet helpers
    // -----------------------------------------------------------------------

    private WrapperPlayServerPlayerInfoUpdate buildTabAddPacket() {
        List<TextureProperty> props = new ArrayList<>();
        if (skin != null) props.add(new TextureProperty("textures", skin[0], skin[1]));

        UserProfile profile = new UserProfile(uuid, getProfileName(), props);
        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        profile, true, 0, GameMode.SURVIVAL, Component.text(displayName), null);

        return new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                Collections.singletonList(info));
    }

    private void sendNametagTeam(User user) {
        String teamName = getProfileName();
        WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo =
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.empty(), Component.empty(), Component.empty(),
                        WrapperPlayServerTeams.NameTagVisibility.NEVER,
                        WrapperPlayServerTeams.CollisionRule.NEVER,
                        null,
                        WrapperPlayServerTeams.OptionData.NONE);
        user.sendPacket(new WrapperPlayServerTeams(
                teamName, WrapperPlayServerTeams.TeamMode.CREATE,
                Optional.of(teamInfo), Collections.singletonList(getProfileName())));
    }

    private List<Equipment> buildEquipmentList() {
        List<Equipment> list = new ArrayList<>();
        for (Map.Entry<EquipmentSlot, ItemStack> e : equipment.entrySet())
            if (e.getValue() != null) list.add(toEquipment(e.getKey(), e.getValue()));
        return list;
    }

    private static Equipment toEquipment(EquipmentSlot slot, ItemStack bukkit) {
        com.github.retrooper.packetevents.protocol.player.EquipmentSlot peSlot =
                switch (slot) {
                    case HAND       -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND;
                    case BOOTS      -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS;
                    case LEGGINGS   -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS;
                    case CHESTPLATE -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE;
                    case HELMET     -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET;
                };
        com.github.retrooper.packetevents.protocol.item.ItemStack peItem =
                (bukkit == null)
                        ? com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY
                        : SpigotConversionUtil.fromBukkitItemStack(bukkit);
        return new Equipment(peSlot, peItem);
    }

    /** Location 1.65 blocks above the NPC's feet — use this as the hologram anchor. */
    public Location nametagLocation() {
        return location.clone().add(0, 1.65, 0);
    }

    private String getProfileName() {
        return uuid.toString().replace("-", "").substring(0, 16);
    }

    // -----------------------------------------------------------------------
    // Nametag text helpers
    // -----------------------------------------------------------------------

    /**
     * Splits a display name on {@code \n} (literal or real newline) into
     * individual lines for the nametag hologram.
     */
    static List<String> splitDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty()) return List.of("");
        return Arrays.asList(displayName.replace("\\n", "\n").split("\n", -1));
    }

    /**
     * Converts a legacy §-formatted string to a MiniMessage string so it
     * renders correctly inside a hologram TEXT_DISPLAY entity.
     */
    static String toNametag(String legacy) {
        if (legacy == null || legacy.isEmpty()) return "";
        Component component = LegacyComponentSerializer.legacySection().deserialize(legacy);
        return MiniMessage.miniMessage().serialize(component);
    }

    // -----------------------------------------------------------------------
    // Skin fetching
    // -----------------------------------------------------------------------

    public static String[] fetchSkin(String playerName) {
        try {

            String uuidResp = getResponse(
                    "https://api.mojang.com/users/profiles/minecraft/" + playerName);
            if (uuidResp == null || uuidResp.isEmpty()) {
                Bukkit.getLogger().warning("[NPC] fetchSkin: empty UUID response for '" + playerName + "'");
                return null;
            }
            com.google.gson.JsonObject parsed =
                    com.google.gson.JsonParser.parseString(uuidResp).getAsJsonObject();
            if (!parsed.has("id")) {
                Bukkit.getLogger().warning("[NPC] fetchSkin: no 'id' in UUID response for '" + playerName + "'");
                return null;
            }
            String id = parsed.get("id").getAsString();

            String profileResp = getResponse(
                    "https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
            if (profileResp == null || profileResp.isEmpty()) {
                Bukkit.getLogger().warning("[NPC] fetchSkin: empty profile response for " + id);
                return null;
            }
            parsed = com.google.gson.JsonParser.parseString(profileResp).getAsJsonObject();
            if (!parsed.has("properties")) {
                Bukkit.getLogger().warning("[NPC] fetchSkin: no 'properties' in profile");
                return null;
            }
            com.google.gson.JsonObject prop = parsed.get("properties")
                    .getAsJsonArray().get(0).getAsJsonObject();
            String value     = prop.get("value").getAsString();
            String signature = prop.get("signature").getAsString();
            return new String[]{value, signature};
        } catch (Exception e) {
            Bukkit.getLogger().warning("[NPC] fetchSkin: exception for '" + playerName + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String getResponse(String urlString) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setReadTimeout(5_000);
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    reader.lines().forEach(sb::append);
                }
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
