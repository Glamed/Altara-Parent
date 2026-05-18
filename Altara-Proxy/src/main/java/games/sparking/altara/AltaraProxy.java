package games.sparking.altara;

import com.velocitypowered.api.proxy.ProxyServer;
import games.sparking.altara.commands.LobbyCommand;
import games.sparking.altara.listeners.MotdListener;
import lombok.Getter;
import org.slf4j.Logger;

public class AltaraProxy extends Altara {

    @Getter private static Object proxyPlugin;

    @Getter private static ProxyServer proxyInstance;

    @Getter private static Logger proxyLogger;

    public AltaraProxy(Object plugin, ProxyServer server, Logger logger) {
        super(SystemType.PROXY);
        proxyPlugin = plugin;
        proxyInstance = server;
        proxyLogger = logger;
        logger.info("Altara Proxy has loaded successfully!");
        init();
    }

    @Override
    public void init() {
        registerCommands();
        registerListener();
    }

    @Override
    public void registerCommands() {
        getProxyInstance().getCommandManager().register(
                getProxyInstance().getCommandManager().metaBuilder("lobby").build(),
                new LobbyCommand()
        );
    }

    @Override
    public void registerListener() {
        getProxyInstance().getEventManager().register(
                getProxyPlugin(),
                new MotdListener()
        );
    }
}
