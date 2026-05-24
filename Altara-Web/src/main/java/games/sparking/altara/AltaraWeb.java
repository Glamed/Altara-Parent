package games.sparking.altara;

import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.configuration.defaults.MainConfig;
import games.sparking.altara.configuration.defaults.ServerConfig;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.task.WebTaskImplementor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
public class AltaraWeb extends Altara {

    public AltaraWeb(MainConfig mainConfig) {
        super(SystemType.WEB, new JsonConfigurationService(), mainConfig, new WebTaskImplementor());
        init();
    }

    @Override
    public void init() {
        log.info("AltaraWeb initialized");
    }

    @Override public void registerCommands()  {}
    @Override public void registerListeners() {}
    @Override public void startServerMonitor() {}
    @Override public void saveMainConfig() {}
    @Override public void loadFiles() {}

    @Override
    public void handleServerInfoUpdate(ServerInfo serverInfo) {
        log.debug("Server info updated: {} ({})", serverInfo.getName(), serverInfo.getState());
    }

    @Override public void updatePermissions(UUID uuid)           {}
    @Override public void updatePermissionsWithRank(Rank rank)   {}
    @Override public List<String> getLocalPermissions(Rank rank) { return Collections.emptyList(); }
    @Override public void saveLocalPermissions(Rank rank)        {}
    @Override public void handleRankDeletion(Rank rank) {
        log.info("Rank deleted: {} ({})", rank.getName(), rank.getUuid());
    }

    @Override public String getServerNameShort() { return serverConfig().getServerNameShort(); }
    @Override public String getServerNameLong()  { return serverConfig().getServerNameLong(); }
    @Override public String getLocalServerName() { return serverConfig().getLocalServerName(); }
    @Override public String getServerGroup()     { return serverConfig().getServerType(); }

    /** Returns the configured {@link ServerConfig}, falling back to defaults if not set in config.json. */
    private ServerConfig serverConfig() {
        ServerConfig cfg = getMainConfig().getServerConfig();
        return cfg != null ? cfg : new ServerConfig();
    }
}
