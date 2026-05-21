package games.sparking.altara.server;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.Time;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
public class ServerInfo {

    private static final Map<String, ServerInfo> servers = new HashMap<>();
    public static final long MAX_TIMEOUT = 5000L;

    private String name = "";
    private String grantScope = "";
    private long lastHeartbeat = System.currentTimeMillis();
    private ServerState state = ServerState.UNKNOWN;
    private int onlinePlayers = 0;
    private int maxPlayers = 0;
    private double tps = 0D;
    private double fullTick = 0D;
    private long usedMemory = 0L;
    private long allocatedMemory = 0L;
    private String host = "localhost";
    private int port = 25565;


    public ServerInfo(String name) {
        this.name = name;
        servers.put(name.toLowerCase(), this);
    }

    public boolean isOnline() {
        return switch (state) {
            case ONLINE, WHITELISTED -> true;
            default -> false;
        };
    }

    public boolean isProxy() {
        return grantScope.equalsIgnoreCase("proxy");
    }

    public int getOnlinePlayers() {
        return isOnline() ? onlinePlayers : 0;
    }

    public static ServerInfo getServerInfo(String name) {
        return servers.getOrDefault(name.toLowerCase(), null);
    }

    public static List<ServerInfo> getServers() {
        return new ArrayList<>(servers.values());
    }

    public static List<ServerInfo> getByGroup(String group) {
        return servers.values().stream()
                .filter(serverInfo -> serverInfo.getGrantScope().equalsIgnoreCase(group))
                .collect(Collectors.toList());
    }

    public static void updateServerInfo(String name, ServerInfo zirconServerInfo) {
        Altara.getSharedInstance().handleServerInfoUpdate(name, zirconServerInfo);
        servers.put(name.toLowerCase(), zirconServerInfo);
    }

    public static int getGlobalPlayerCount() {
        return servers.values().stream()
                .filter(server -> !server.isProxy())
                .mapToInt(ServerInfo::getOnlinePlayers)
                .sum();
    }
}