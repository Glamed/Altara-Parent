package games.sparking.altara;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "altaraproxy",
        name = "Altara Proxy",
        version = "1.0-SNAPSHOT",
        url = "https://sparking.games",
        description = "Altara Proxy Manager",
        authors = {"Glamify"}
)
public class AltaraProxyPlugin {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public AltaraProxyPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        logger.info("Altara Proxy initialized.");

        new AltaraProxy(this, server, logger);
    }
}