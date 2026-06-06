package games.sparking.altara;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.gson.GsonBuilder;
import games.sparking.altara.chat.ChatChannelRegistry;
import games.sparking.altara.chat.ChatListener;
import games.sparking.altara.chat.command.ChatCommands;
import games.sparking.altara.chat.impl.*;
import games.sparking.altara.command.BuildVersionCommand;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import games.sparking.altara.configuration.entry.LocalPermissionConfig;
import games.sparking.altara.configuration.entry.LocalPermissionEntry;
import games.sparking.altara.gamemode.GamemodeCommand;
import games.sparking.altara.hologram.HologramService;
import games.sparking.altara.hologram.command.HologramCommands;
import games.sparking.altara.hologram.command.parameter.HologramParameter;
import games.sparking.altara.hologram.listener.HologramClickListener;
import games.sparking.altara.hologram.listener.HologramListener;
import games.sparking.altara.hologram.statics.StaticHologram;
import games.sparking.altara.logging.PaperLogger;
import games.sparking.altara.menu.listener.MenuListener;
import games.sparking.altara.messaging.MessageCommands;
import games.sparking.altara.npc.NPC;
import games.sparking.altara.npc.NPCService;
import games.sparking.altara.npc.command.NPCCommands;
import games.sparking.altara.npc.command.parameter.EquipmentSlotParameter;
import games.sparking.altara.npc.command.parameter.NPCParameter;
import games.sparking.altara.npc.equipment.EquipmentSlot;
import games.sparking.altara.npc.listener.NPCClickListener;
import games.sparking.altara.npc.listener.NPCListener;
import games.sparking.altara.permission.PermissionService;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.playersetting.PlayerSettingService;
import games.sparking.altara.playersetting.command.SettingsCommands;
import games.sparking.altara.playersetting.listener.PlayerSettingListener;
import games.sparking.altara.profile.BukkitProfileService;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.profile.UnloadedProfile;
import games.sparking.altara.profile.parameters.ProfileParameter;
import games.sparking.altara.profile.parameters.UnloadedProfileParameter;
import games.sparking.altara.profiler.ProfilerListener;
import games.sparking.altara.profiler.command.ProfilerCommand;
import games.sparking.altara.punishment.commands.PunishCommand;
import games.sparking.altara.punishment.listeners.PunishmentListener;
import games.sparking.altara.queue.Queue;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.queue.commands.QueueCommands;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.parameter.RankParameter;
import games.sparking.altara.reboot.RebootCommands;
import games.sparking.altara.scoreboard.ScoreboardListener;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.server.command.ServerMonitorCommands;
import games.sparking.altara.server.packet.UpdateServerPacket;
import games.sparking.altara.server.parameter.AllServersParameter;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateTask;
import games.sparking.altara.task.impl.BukkitTaskImplementor;
import games.sparking.altara.updater.FileUpdater;
import games.sparking.altara.utils.json.adapter.ItemStackAdapter;
import games.sparking.altara.utils.json.adapter.UUIDAdapter;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AltaraPaper extends Altara {

    @Getter private static JavaPlugin plugin;
    @Getter private static AltaraPaper paperInstance;

    @Getter private ServerInfo serverInfo;

    @Getter private PermissionService permissionService;
    @Getter private BukkitProfileService bukkitProfileService;

    @Getter private LocalPermissionConfig localPermissionConfig;
    @Getter private final LocalConfig localConfig;

    @Getter private Queue queue;
    @Getter private QueueService queueService;

    @Getter private HologramService hologramService;
    @Getter private HologramClickListener hologramClickListener;

    @Getter private NPCService npcService;
    @Getter private NPCClickListener npcClickListener;

    public AltaraPaper(JavaPlugin plugin, ConfigurationService configurationService, LocalConfig localConfig) {
        super(SystemType.PAPER, configurationService, localConfig, new BukkitTaskImplementor(plugin));
        AltaraPaper.plugin = plugin;
        AltaraPaper.paperInstance = this;
        setLogger(new PaperLogger());
        this.localConfig = localConfig;

        getPlugin().getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(), "BungeeCord");

        init();
    }

    @Override
    public void init() {
        loadFiles();

        JsonConfigurationService.setGson(new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(UUID.class, new UUIDAdapter())
                .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackAdapter())
                .create());

        UpdateTask.start();

        this.queue = new Queue();
        this.queueService = new QueueService();
        queueService.startTask();
        PlayerSettingService.registerProvider(new AltaraSettings());

        this.hologramService = new HologramService(plugin, getConfigurationService());
        hologramService.load();

        this.npcService = new NPCService(plugin, getConfigurationService());
        npcService.load();

        registerChatChannels();

    }

    // ── Chat channels ──────────────────────────────────────────────────────────

    /**
     * Registers built-in chat channels in priority order.
     * Add custom channels here or load them from config as needed.
     */
    private void registerChatChannels() {
        // Registration order matters when channels share a prefix — higher
        // priority channels are checked first in ChatChannelRegistry.getByPrefix().
        ChatChannelRegistry.register(GlobalChannel.getInstance());     // ! — default
        ChatChannelRegistry.register(LocalChannel.getInstance());      // #
        ChatChannelRegistry.register(StaffChannel.getInstance());      // @
        ChatChannelRegistry.register(AdminChannel.getInstance());      // $
        ChatChannelRegistry.register(ShadowMuteChannel.getInstance()); // system-only, no prefix
    }

    @Override
    public void registerCommands() {
        CommandService.registerParameter(Profile.class, new ProfileParameter());
        CommandService.registerParameter(UnloadedProfile.class, new UnloadedProfileParameter());
        CommandService.registerParameter(Rank.class, new RankParameter());
        CommandService.registerParameter(ServerInfo.class, new AllServersParameter());
        CommandService.registerParameter(StaticHologram.class, new HologramParameter(hologramService));
        CommandService.registerParameter(NPC.class, new NPCParameter());
        CommandService.registerParameter(EquipmentSlot.class, new EquipmentSlotParameter());

        CommandService.register(AltaraPaper.getPlugin(),
                new GamemodeCommand(),
                new BuildVersionCommand(),
                new PunishCommand(),
                new ProfilerCommand(),
                new SettingsCommands(),
                new HologramCommands(),
                new NPCCommands(),
                new RebootCommands(),
                new ServerMonitorCommands(),
                new QueueCommands(),
                new ChatCommands(),
                new MessageCommands()
        );
    }

    @Override
    public void registerListeners() {
        Arrays.asList(
                new ChatListener(),
                new MenuListener(),
                new PunishmentListener(),
                new ProfilerListener(),
                new ScoreboardListener(),
                new HologramListener(),
                new NPCListener(npcService),
                new PlayerSettingListener()
        ).forEach(listener -> getPlugin().getServer().getPluginManager().registerEvents(listener, getPlugin()));

        // Hologram click detection requires a PacketEvents listener (holograms are fake entities).
        hologramClickListener = new HologramClickListener();
        PacketEvents.getAPI().getEventManager().registerListeners(hologramClickListener);

        // NPC click detection via PacketEvents (NPCs are fake entities too).
        npcClickListener = new NPCClickListener();
        PacketEvents.getAPI().getEventManager().registerListeners(npcClickListener);

        new FileUpdater();
    }

    @Override
    public void loadFiles() {
        if (getConfigurationService() == null) {
            getLogger().error("ConfigurationService is null in loadFiles!");
            return;
        }
        this.localPermissionConfig = getConfigurationService().loadConfiguration(LocalPermissionConfig.class,
                new File(getPlugin().getDataFolder(), "permissions.json"));
    }

    @Override
    public void saveMainConfig() {
        try {
            getConfigurationService().saveConfiguration(this.getMainConfig(),
                    new File(getPlugin().getDataFolder(), "config.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void startServerMonitor() {
        this.serverInfo = new ServerInfo(getLocalServerName());

        Tasks.runTimerAsync(() -> {
            ServerInfo.getServers().stream()
                    .filter(server -> System.currentTimeMillis() - server.getLastHeartbeat() > ServerInfo.MAX_TIMEOUT
                            && server.getState() != ServerState.HEARTBEAT_TIMEOUT
                            && server.getState() != ServerState.OFFLINE)
                    .forEach(server -> server.setState(ServerState.HEARTBEAT_TIMEOUT));

            double tps = Bukkit.getServer().getTPS()[0]; // 1 min avg
            double mspt = Bukkit.getServer().getAverageTickTime(); // 1 min avg

            serverInfo.setLastHeartbeat(System.currentTimeMillis());
            serverInfo.setGroup(getServerGroup());
            serverInfo.setState(Bukkit.getServer().isWhitelistEnforced() ? ServerState.WHITELISTED : ServerState.ONLINE);
            serverInfo.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
            serverInfo.setMaxPlayers(Bukkit.getMaxPlayers());
            serverInfo.setTps(tps);
            serverInfo.setFullTick(mspt);
            serverInfo.setUsedMemory((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 2L / 1048576L);
            serverInfo.setAllocatedMemory(Runtime.getRuntime().totalMemory() / 1048576L);
            serverInfo.setQueueEnabled(localConfig.isQueueEnabled());
            serverInfo.setQueuePaused(localConfig.isQueuePaused());
            serverInfo.setQueueRate(localConfig.getQueueRate());
            new UpdateServerPacket(serverInfo).publish();
        }, 20L, 1L);
    }

    @Override
    public void dispatchConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public void updatePermissions(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null)
            permissionService.updatePermissions(Bukkit.getPlayer(uuid));
    }

    @Override
    public void updatePermissionsWithRank(Rank rank) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Profile profile = getProfileService().getProfile(player);
            if (profile.hasGrantOf(rank))
                permissionService.updatePermissions(player);
        }
    }

    public void saveLocalPermissionConfig() {
        try {
            getConfigurationService().saveConfiguration(this.localPermissionConfig,
                    new File(getPlugin().getDataFolder(), "permissions.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getLocalPermissions(Rank rank) {
        if (localPermissionConfig == null) return new ArrayList<>();
        LocalPermissionEntry entry = localPermissionConfig.getEntry(rank);
        if (entry != null) {
            return entry.getPermissions();
        }

        entry = new LocalPermissionEntry();
        entry.setUuid(rank.getUuid().toString());
        localPermissionConfig.getRankPermissions().add(entry);
        saveLocalPermissionConfig();
        return new ArrayList<>();
    }

    @Override
    public void saveLocalPermissions(Rank rank) {
        if (localPermissionConfig == null) return;
        LocalPermissionEntry entry = localPermissionConfig.getEntry(rank);
        if (entry != null) {
            entry.setPermissions(new ArrayList<>(rank.getLocalPermissions()));
        } else {
            entry = new LocalPermissionEntry();
            entry.setUuid(rank.getUuid().toString());
            entry.setPermissions(new ArrayList<>(rank.getLocalPermissions()));
            localPermissionConfig.getRankPermissions().add(entry);
        }
        saveLocalPermissionConfig();
    }

    @Override
    public void handleRankDeletion(Rank rank) {
        if (localPermissionConfig == null) return;
        LocalPermissionEntry entry = localPermissionConfig.getEntry(rank);
        if (entry != null) {
            localPermissionConfig.getRankPermissions().remove(entry);
            saveLocalPermissionConfig();
        }
    }

    @Override
    public String getServerNameShort() {
        return this.getMainConfig().getServerConfig().getServerNameShort();
    }

    @Override
    public String getServerNameLong() {
        return this.getMainConfig().getServerConfig().getServerNameLong();
    }

    @Override
    public String getLocalServerName() {
        return this.getMainConfig().getServerConfig().getLocalServerName();
    }

    @Override
    public String getServerGroup() {
        return this.getMainConfig().getServerConfig().getServerType();
    }

}


