package games.sparking.altara.service;

import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.rank.packets.RankCreatePacket;
import games.sparking.altara.rank.packets.RankDeletePacket;
import games.sparking.altara.rank.packets.RankUpdatePacket;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankWebService {

    private final RankRepository rankRepository;
    private final RedisService redisService;

    /** Cached under key {@code "all"} — evicted on any mutation. */
    @Cacheable(value = CacheConfig.RANKS, key = "'all'")
    public List<JsonObject> getAllRanks() {
        return rankRepository.findAll();
    }

    @Cacheable(value = CacheConfig.RANKS, key = "#uuid.toString()")
    public Optional<JsonObject> getRank(UUID uuid) {
        return rankRepository.findByUuid(uuid.toString());
    }

    /** Evict the entire {@code ranks} cache — both the "all" list and any individual entries. */
    @CacheEvict(value = CacheConfig.RANKS, allEntries = true)
    public JsonObject createRank(JsonObject rank) {
        JsonObject created = rankRepository.insert(rank);
        publishCreate(rank.get("uuid").getAsString());
        return created;
    }

    /**
     * Upsert — used by {@code Rank.save()} which sends {@code PUT /api/rank} (UUID in body).
     */
    @CacheEvict(value = CacheConfig.RANKS, allEntries = true)
    public JsonObject upsertRank(JsonObject rank) {
        JsonObject result = rankRepository.upsert(rank);
        publishUpdate(rank.get("uuid").getAsString());
        return result;
    }

    @CacheEvict(value = CacheConfig.RANKS, allEntries = true)
    public boolean deleteRank(UUID uuid) {
        boolean deleted = rankRepository.deleteByUuid(uuid.toString());
        if (deleted) publishDelete(uuid.toString());
        return deleted;
    }

    // ------------------------------------------------------------------
    // Redis notifications
    // ------------------------------------------------------------------

    private void publishCreate(String uuid) {
        try {
            redisService.publish(new RankCreatePacket(UUID.fromString(uuid)));
        } catch (Exception e) {
            log.warn("Could not publish RankCreatePacket: {}", e.getMessage());
        }
    }

    private void publishUpdate(String uuid) {
        try {
            redisService.publish(new RankUpdatePacket(UUID.fromString(uuid)));
        } catch (Exception e) {
            log.warn("Could not publish RankUpdatePacket: {}", e.getMessage());
        }
    }

    private void publishDelete(String uuid) {
        try {
            redisService.publish(new RankDeletePacket(UUID.fromString(uuid)));
        } catch (Exception e) {
            log.warn("Could not publish RankDeletePacket: {}", e.getMessage());
        }
    }
}
