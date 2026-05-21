package games.sparking.altara;

import games.sparking.altara.command.BuildVersionCommand;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.gamemode.GamemodeCommand;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.server.UpdateServerPacket;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateTask;
import games.sparking.altara.task.impl.BukkitTaskImplementor;
import games.sparking.altara.updater.FileUpdater;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraPaper extends Altara {

    @Getter private static JavaPlugin paperInstance;

    @Getter private ServerInfo serverInfo;

    public AltaraPaper(JavaPlugin paperInstance) {
        super(SystemType.PAPER);
        AltaraPaper.paperInstance = paperInstance;

        init();
    }

    @Override
    public void init() {
        Tasks.setTaskImplementor(new BukkitTaskImplementor(paperInstance));
        UpdateTask.start();
    }

    @Override
    public void registerCommands() {
        CommandService.register(AltaraPaper.getPaperInstance(),
                new GamemodeCommand(),
                new BuildVersionCommand()
        );
    }

    @Override
    public void registerListener() {
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
            serverInfo.setState(ServerState.ONLINE);
            serverInfo.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
            serverInfo.setMaxPlayers(Bukkit.getMaxPlayers());
            serverInfo.setTps(tps);
            serverInfo.setFullTick(mspt);
            serverInfo.setUsedMemory((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 2L / 1048576L);
            serverInfo.setAllocatedMemory(Runtime.getRuntime().totalMemory() / 1048576L);
            new UpdateServerPacket(serverInfo).publish();
        }, 20L, 1L);
    }

}

