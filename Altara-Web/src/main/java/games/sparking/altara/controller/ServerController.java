package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.server.ServerInfo;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import static games.sparking.altara.controller.Responses.notFound;
import static games.sparking.altara.controller.Responses.ok;

/**
 * Servers known from Redis heartbeats ({@code UpdateServerPacket}).
 *
 * <pre>
 *   GET /api/server           every server
 *   GET /api/server/{name}    one server
 *   GET /api/server/count     {"count"} players network-wide
 *   GET /api/server/health    {"status": "UP"} (no auth, for load-balancer probes)
 * </pre>
 */
@Controller("/api/server")
@Produces(MediaType.APPLICATION_JSON)
public class ServerController {

    @Get
    public HttpResponse<String> getAllServers() {
        JsonArray array = new JsonArray();
        for (ServerInfo server : ServerInfo.getServers()) array.add(toJson(server));
        return ok(array.toString());
    }

    @Get("/{name}")
    public HttpResponse<String> getServer(String name) {
        ServerInfo server = ServerInfo.getServerInfo(name);
        return server == null ? notFound("Server not found: " + name) : ok(toJson(server));
    }

    @Get("/count")
    public HttpResponse<String> getGlobalPlayerCount() {
        JsonObject json = new JsonObject();
        json.addProperty("count", ServerInfo.getGlobalPlayerCount());
        return ok(json);
    }

    @Get("/health")
    public HttpResponse<String> health() {
        return ok("{\"status\":\"UP\"}");
    }

    private static JsonObject toJson(ServerInfo server) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", server.getName());
        obj.addProperty("group", server.getGroup());
        obj.addProperty("state", server.getState().name());
        obj.addProperty("online", server.isOnline());
        obj.addProperty("onlinePlayers", server.getOnlinePlayers());
        obj.addProperty("maxPlayers", server.getMaxPlayers());
        obj.addProperty("tps", server.getTps());
        obj.addProperty("fullTick", server.getFullTick());
        obj.addProperty("usedMemory", server.getUsedMemory());
        obj.addProperty("allocatedMemory", server.getAllocatedMemory());
        obj.addProperty("host", server.getHost());
        obj.addProperty("port", server.getPort());
        obj.addProperty("lastHeartbeat", server.getLastHeartbeat());
        return obj;
    }
}
