package games.sparking.altara;

import com.velocitypowered.api.proxy.ProxyServer;
import games.sparking.altara.commands.LobbyCommand;
import games.sparking.altara.commands.SendCommand;
import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.listeners.MotdListener;
import lombok.Getter;
import org.slf4j.Logger;

public class AltaraProxy extends Altara {

    @Getter private static Object proxyPlugin;

    @Getter private static ProxyServer proxyInstance;

    @Getter private static Logger proxyLogger;

    public AltaraProxy(Object plugin, ProxyServer server, Logger logger, ConfigurationService configurationService, MainConfig mainConfig) {
        super(SystemType.PROXY, configurationService, mainConfig);
        proxyPlugin = plugin;
        proxyInstance = server;
        proxyLogger = logger;
        Altara.setProxyServer(server);
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
    }

    @Override
    public void startServerMonitor() {

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
