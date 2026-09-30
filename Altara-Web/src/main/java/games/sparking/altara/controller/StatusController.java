package games.sparking.altara.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.mongodb.client.MongoDatabase;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.Statics;
import io.micronaut.cache.CacheManager;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import org.bson.Document;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

import static games.sparking.altara.controller.Responses.ok;

/** {@code GET /api/status}: MongoDB, Redis, request and cache health for this node. */
@Controller("/api/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusController {

    private final MongoDatabase database;
    private final CacheManager<?> cacheManager;

    public StatusController(MongoDatabase database, CacheManager<?> cacheManager) {
        this.database = database;
        this.cacheManager = cacheManager;
    }

    @Get
    public HttpResponse<String> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("server", Altara.getSharedInstance().getLocalServerName());
        status.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        status.put("mongodb", mongoStatus());
        status.put("redis", redisStatus());
        status.put("requestHandler", requestHandlerStatus());
        status.put("serverCount", ServerInfo.getServers().size());
        status.put("globalPlayerCount", ServerInfo.getGlobalPlayerCount());
        status.put("cache", cacheStats());
        return ok(Statics.GSON.toJson(status));
    }

    private Map<String, Object> mongoStatus() {
        Map<String, Object> mongo = new LinkedHashMap<>();
        try {
            database.runCommand(new Document("ping", 1));
            mongo.put("status", "UP");
        } catch (Exception e) {
            mongo.put("status", "DOWN");
            mongo.put("error", e.getMessage());
        }
        return mongo;
    }

    private static Map<String, Object> redisStatus() {
        Map<String, Object> redis = new LinkedHashMap<>();
        redis.put("down", RedisService.isDown());
        redis.put("lastExecution", RedisService.getLastExecution());
        redis.put("lastPacket", RedisService.getLastPacket());
        redis.put("lastPacketName", RedisService.getLastPacketName());
        redis.put("lastError", RedisService.getLastError());
        return redis;
    }

    private static Map<String, Object> requestHandlerStatus() {
        Map<String, Object> handler = new LinkedHashMap<>();
        handler.put("apiDown", RequestHandler.isApiDown());
        handler.put("totalRequests", RequestHandler.getTotalRequests());
        handler.put("lastRequest", RequestHandler.getLastRequest());
        handler.put("lastLatency", RequestHandler.getLastLatency());
        handler.put("averageLatency", RequestHandler.getAverageLatency());
        handler.put("backLogSize", RequestHandler.getBackLogSize());
        handler.put("lastError", RequestHandler.getLastError());
        return handler;
    }

    private Map<String, Object> cacheStats() {
        Map<String, Object> all = new LinkedHashMap<>();
        for (String name : cacheManager.getCacheNames()) {
            if (!(cacheManager.getCache(name).getNativeCache() instanceof Cache<?, ?> cache)) continue;
            CacheStats stats = cache.stats();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("size", cache.estimatedSize());
            entry.put("hitCount", stats.hitCount());
            entry.put("missCount", stats.missCount());
            entry.put("hitRate", stats.hitRate());
            entry.put("evictions", stats.evictionCount());
            entry.put("loadCount", stats.loadCount());
            entry.put("avgLoadMs", stats.averageLoadPenalty() / 1_000_000.0);
            all.put(name, entry);
        }
        return all;
    }
}
