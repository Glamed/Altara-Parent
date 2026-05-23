package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.Statics;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of servers that have pushed heartbeat packets via Redis.
 * The actual {@link ServerInfo} map is populated by {@code UpdateServerPacket.receive()}.
 */
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllServers() {
        JsonArray array = new JsonArray();
        for (ServerInfo server : ServerInfo.getServers()) {
            array.add(serverToJson(server));
        }
        return ok(array.toString());
    }

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getServer(@PathVariable String name) {
        ServerInfo server = ServerInfo.getServerInfo(name);
        if (server == null) {
            return ResponseEntity.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Server not found: " + name + "\"}");
        }
        return ok(Statics.GSON.toJson(serverToJson(server)));
    }

    @GetMapping(value = "/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGlobalPlayerCount() {
        JsonObject json = new JsonObject();
        json.addProperty("count", ServerInfo.getGlobalPlayerCount());
        return ok(Statics.GSON.toJson(json));
    }

    // ------------------------------------------------------------------
    // Health check — no auth required (useful for load-balancer probes)
    // ------------------------------------------------------------------
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ok("{\"status\":\"UP\"}");
    }

    private static JsonObject serverToJson(ServerInfo s) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", s.getName());
        obj.addProperty("group", s.getGroup());
        obj.addProperty("state", s.getState().name());
        obj.addProperty("online", s.isOnline());
        obj.addProperty("onlinePlayers", s.getOnlinePlayers());
        obj.addProperty("maxPlayers", s.getMaxPlayers());
        obj.addProperty("tps", s.getTps());
        obj.addProperty("fullTick", s.getFullTick());
        obj.addProperty("usedMemory", s.getUsedMemory());
        obj.addProperty("allocatedMemory", s.getAllocatedMemory());
        obj.addProperty("host", s.getHost());
        obj.addProperty("port", s.getPort());
        obj.addProperty("lastHeartbeat", s.getLastHeartbeat());
        return obj;
    }

    private ResponseEntity<String> ok(String json) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }
}

