package games.sparking.altara;

import com.velocitypowered.api.proxy.ProxyServer;
import games.sparking.altara.commands.LobbyCommand;
import games.sparking.altara.commands.SendCommand;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.listeners.MotdListener;
import games.sparking.altara.listeners.PermissionListener;
import games.sparking.altara.logging.VelocityLogger;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.DefaultTaskImplementor;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public class AltaraProxy extends Altara {

    @Getter private static Object proxyPlugin;

    @Getter private static ProxyServer proxyInstance;

    @Getter private static Logger proxyLogger;

    public AltaraProxy(Object plugin, ProxyServer server, Logger logger, ConfigurationService configurationService, MainConfig mainConfig) {
        super(SystemType.PROXY, configurationService, mainConfig, new DefaultTaskImplementor());
        proxyPlugin = plugin;
        proxyInstance = server;
        proxyLogger = logger;
        setLogger(new VelocityLogger());
        logger.info("Altara Proxy has loaded successfully!");
        init();
    }

    @Override
    public void init() {
        registerCommands();
        registerListeners();
    }

    @Override
    public void registerCommands() {
        getProxyInstance().getCommandManager().register(
                getProxyInstance().getCommandManager().metaBuilder("lobby").build(),
                new LobbyCommand()
        );
        getProxyInstance().getCommandManager().register(
                getProxyInstance().getCommandManager().metaBuilder("send").build(),
                new SendCommand()
        );
    }

    @Override
    public void registerListeners() {
        getProxyInstance().getEventManager().register(
                getProxyPlugin(),
                new MotdListener()
        );
        getProxyInstance().getEventManager().register(
                getProxyPlugin(),
                new PermissionListener()
        );
    }

    @Override
    public void loadFiles() {

    }

    @Override
    public void saveMainConfig() {

    }

    @Override
    public void startServerMonitor() {

    }

    @Override
    public void dispatchConsoleCommand(String command) {
        getProxyInstance().getCommandManager().executeAsync(
                getProxyInstance().getConsoleCommandSource(),
                command
        );
    }

    @Override
    public void updatePermissions(UUID uuid) {

    }

    @Override
    public void updatePermissionsWithRank(Rank rank) {

    }

    @Override
    public List<String> getLocalPermissions(Rank rank) {
        return List.of();
    }

    @Override
    public void saveLocalPermissions(Rank rank) {

    }

    @Override
    public void handleRankDeletion(Rank rank) {

    }

    @Override
    public String getServerNameShort() {
        return "";
    }

    @Override
    public String getServerNameLong() {
        return "";
    }

    @Override
    public String getLocalServerName() {
        return "";
    }

    @Override
    public String getServerGroup() {
        return "";
    }
}
