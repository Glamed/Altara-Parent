package games.sparking.altara;

import games.sparking.altara.chat.ChatListener;
import games.sparking.altara.command.BuildVersionCommand;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.configuration.entry.LocalConfig;
import games.sparking.altara.configuration.entry.LocalPermissionConfig;
import games.sparking.altara.configuration.entry.LocalPermissionEntry;
import games.sparking.altara.gamemode.GamemodeCommand;
import games.sparking.altara.menu.listener.MenuListener;
import games.sparking.altara.permission.PermissionService;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.profile.UnloadedProfile;
import games.sparking.altara.profile.parameters.ProfileParameter;
import games.sparking.altara.profile.parameters.UnloadedProfileParameter;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.server.UpdateServerPacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateTask;
import games.sparking.altara.task.impl.BukkitTaskImplementor;
import games.sparking.altara.updater.FileUpdater;
import lombok.Getter;
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

    @Getter private static JavaPlugin paperInstance;

    @Getter private ServerInfo serverInfo;

    private PermissionService permissionService;
    private BukkitProfileService bukkitProfileService;

    @Getter private static LocalPermissionConfig localPermissionConfig;
    @Getter private final LocalConfig localConfig;

    public AltaraPaper(JavaPlugin paperInstance, ConfigurationService configurationService, LocalConfig localConfig) {
        super(SystemType.PAPER, configurationService, localConfig);
        AltaraPaper.paperInstance = paperInstance;
        this.localConfig = localConfig;

        init();
    }

    @Override
    public void init() {
        Tasks.setTaskImplementor(new BukkitTaskImplementor(paperInstance));
        UpdateTask.start();
    }

    @Override
    public void registerCommands() {
        CommandService.registerParameter(Profile.class, new ProfileParameter());
        CommandService.registerParameter(UnloadedProfile.class, new UnloadedProfileParameter());
        CommandService.registerParameter(Rank.class, new RankParameter());

        CommandService.register(AltaraPaper.getPaperInstance(),
                new GamemodeCommand(),
                new BuildVersionCommand()
        );
    }

    @Override
    public void registerListeners() {
        Arrays.asList(
                new ChatListener(),
                new MenuListener()
        ).forEach(listener -> getPaperInstance().getServer().getPluginManager().registerEvents(listener, getPaperInstance()));
        new FileUpdater();
    }

    @Override
    public void startServerMonitor() {
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
                    new File(getPaperInstance().getDataFolder(), "permissions.json"));
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


