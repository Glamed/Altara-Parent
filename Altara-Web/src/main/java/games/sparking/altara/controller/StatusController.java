package games.sparking.altara.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.server.ServerInfo;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes runtime status for the Altara web node.
 *
 * <pre>
 *   GET /api/status   — full status summary (MongoDB, Redis, servers, cache, request stats)
 * </pre>
 *
 * This endpoint is protected by {@link games.sparking.altara.filter.AuthFilter}
 * like every other API endpoint.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final MongoTemplate mongoTemplate;
    private final CacheManager cacheManager;

    public StatusController(MongoTemplate mongoTemplate, CacheManager cacheManager) {
        this.mongoTemplate = mongoTemplate;
        this.cacheManager = cacheManager;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> status() {
        Map<String, Object> status = new LinkedHashMap<>();

        // Server name comes from config.json via Altara singleton — no Spring @Value needed
        status.put("server", Altara.getSharedInstance().getLocalServerName());
        status.put("uptime", System.currentTimeMillis());

        // MongoDB
        status.put("mongodb", mongoStatus());

        // Redis
        status.put("redis", redisStatus());

        // HTTP request handler stats
        status.put("requestHandler", requestHandlerStatus());

        // Server summary
        status.put("serverCount", ServerInfo.getServers().size());
        status.put("globalPlayerCount", ServerInfo.getGlobalPlayerCount());

        // Caffeine cache stats
        status.put("cache", cacheStats());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(status));
    }

    // ------------------------------------------------------------------
    // Helpers — build sub-sections
    // ------------------------------------------------------------------

    private Map<String, Object> mongoStatus() {
        Map<String, Object> mongo = new LinkedHashMap<>();
        try {
            mongoTemplate.getDb().runCommand(new org.bson.Document("ping", 1));
            mongo.put("status", "UP");
        } catch (Exception e) {
            mongo.put("status", "DOWN");
            mongo.put("error", e.getMessage());
        }
        return mongo;
    }

    private Map<String, Object> redisStatus() {
        Map<String, Object> redis = new LinkedHashMap<>();
        redis.put("down", RedisService.isDown());
        redis.put("lastExecution", RedisService.getLastExecution());
        redis.put("lastPacket", RedisService.getLastPacket());
        redis.put("lastPacketName", RedisService.getLastPacketName());
        redis.put("lastError", RedisService.getLastError());
        return redis;
    }

    private Map<String, Object> requestHandlerStatus() {
        Map<String, Object> rh = new LinkedHashMap<>();
        rh.put("apiDown", RequestHandler.isApiDown());
        rh.put("totalRequests", RequestHandler.getTotalRequests());
        rh.put("lastRequest", RequestHandler.getLastRequest());
        rh.put("lastLatency", RequestHandler.getLastLatency());
        rh.put("averageLatency", RequestHandler.getAverageLatency());
        rh.put("backLogSize", RequestHandler.getBackLogSize());
        rh.put("lastError", RequestHandler.getLastError());
        return rh;
    }

    private Map<String, Object> cacheStats() {
        Map<String, Object> all = new LinkedHashMap<>();
        for (String name : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(name);
            if (springCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("size",        nativeCache.estimatedSize());
                entry.put("hitCount",    stats.hitCount());
                entry.put("missCount",   stats.missCount());
                entry.put("hitRate",     stats.hitRate());
                entry.put("evictions",   stats.evictionCount());
                entry.put("loadCount",   stats.loadCount());
                entry.put("avgLoadMs",   stats.averageLoadPenalty() / 1_000_000.0);
                all.put(name, entry);
            }
        }
        return all;
    }

    // ------------------------------------------------------------------
    // Minimal JSON serialisation — avoids pulling in Gson dependency here
    // ------------------------------------------------------------------

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object v = entry.getValue();
            if (v instanceof Map<?, ?> nested) {
                //noinspection unchecked
                sb.append(toJson((Map<String, Object>) nested));
            } else if (v instanceof String s) {
                sb.append("\"").append(s.replace("\"", "\\\"")).append("\"");
            } else {
                sb.append(v);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
