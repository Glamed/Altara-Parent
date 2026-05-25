package games.sparking.altara;

import games.sparking.altara.chat.ChatListener;
import games.sparking.altara.command.BuildVersionCommand;
import games.sparking.altara.npc.NPCBukkitListener;
import games.sparking.altara.profiler.ProfilerListener;
import games.sparking.altara.profiler.command.ProfilerCommand;
import games.sparking.altara.punishment.commands.PunishCommand;
import games.sparking.altara.punishment.listener.PunishmentChatListener;
import games.sparking.altara.punishment.listener.PunishmentLoginListener;
import games.sparking.altara.logging.PaperLogger;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.LocalConfig;
import games.sparking.altara.configuration.entry.LocalPermissionConfig;
import games.sparking.altara.configuration.entry.LocalPermissionEntry;
import games.sparking.altara.gamemode.GamemodeCommand;
import games.sparking.altara.menu.listener.MenuListener;
import games.sparking.altara.permission.PermissionService;
import games.sparking.altara.profile.BukkitProfileService;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.profile.UnloadedProfile;
import games.sparking.altara.profile.parameters.ProfileParameter;
import games.sparking.altara.profile.parameters.UnloadedProfileParameter;
import games.sparking.altara.queue.Queue;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.parameter.RankParameter;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.server.packet.UpdateServerPacket;
import games.sparking.altara.server.parameter.AllServersParameter;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateTask;
import games.sparking.altara.task.impl.BukkitTaskImplementor;
import games.sparking.altara.updater.FileUpdater;
import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

    public AltaraPaper(JavaPlugin plugin, ConfigurationService configurationService, LocalConfig localConfig) {
        super(SystemType.PAPER, configurationService, localConfig, new BukkitTaskImplementor(plugin));
        AltaraPaper.plugin = plugin;
        AltaraPaper.paperInstance = this;
        setLogger(new PaperLogger());
        this.localConfig = localConfig;

        init();
    }

    @Override
    public void init() {
        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
        APIConfig settings = new APIConfig(PacketEvents.getAPI())
                .debugMode()
                .tickTickables()
                .usePlatformLogger();
        EntityLib.init(platform, settings);

        UpdateTask.start();

        this.queue = new Queue();
        this.queueService = new QueueService();
        queueService.startTask();
    }

    @Override
    public void registerCommands() {
        CommandService.registerParameter(Profile.class, new ProfileParameter());
        CommandService.registerParameter(UnloadedProfile.class, new UnloadedProfileParameter());
        CommandService.registerParameter(Rank.class, new RankParameter());
        CommandService.registerParameter(ServerInfo.class, new AllServersParameter());

        CommandService.register(AltaraPaper.getPlugin(),
                new GamemodeCommand(),
                new BuildVersionCommand(),
                new PunishCommand(),
                new ProfilerCommand()
        );
    }

    @Override
    public void registerListeners() {
        Arrays.asList(
                new ChatListener(),
                new MenuListener(),
                new PunishmentLoginListener(),
                new PunishmentChatListener(),
                new ProfilerListener(),
                new NPCBukkitListener()
        ).forEach(listener -> getPlugin().getServer().getPluginManager().registerEvents(listener, getPlugin()));
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
            serverInfo.setGroup("Lobby");
            serverInfo.setState(Bukkit.getServer().isWhitelistEnforced() ? ServerState.WHITELISTED : ServerState.ONLINE);
            serverInfo.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
            serverInfo.setMaxPlayers(Bukkit.getMaxPlayers());
            serverInfo.setTps(tps);
            serverInfo.setFullTick(mspt);
            serverInfo.setUsedMemory((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 2L / 1048576L);
            serverInfo.setAllocatedMemory(Runtime.getRuntime().totalMemory() / 1048576L);
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


