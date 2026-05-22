package games.sparking.altara;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private final Path dataDirectory;

    @Inject
    public AltaraProxyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            // Velocity does NOT create this automatically
            Files.createDirectories(dataDirectory);
        } catch (Exception e) {
            logger.error("Failed to create data directory", e);
        }

        File configFile = dataDirectory.resolve("config.json").toFile();

        JsonConfigurationService configurationService = new JsonConfigurationService();
        MainConfig mainConfig = configurationService.loadConfiguration(
                MainConfig.class,
                configFile
        );

        new AltaraProxy(this, server, logger, configurationService, mainConfig);
    }
}