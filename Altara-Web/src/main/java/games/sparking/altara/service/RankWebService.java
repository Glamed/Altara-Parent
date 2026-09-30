package games.sparking.altara.service;

import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.rank.packets.RankCreatePacket;
import games.sparking.altara.rank.packets.RankDeletePacket;
import games.sparking.altara.rank.packets.RankUpdatePacket;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.repository.RankRepository;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Ranks.  Any write clears the whole rank cache (the full list and single entries). */
@Singleton
@RequiredArgsConstructor
@Slf4j
public class RankWebService {

    private final RankRepository rankRepository;
    private final RedisService redisService;

    @Cacheable(CacheConfig.RANKS)
    public List<JsonObject> getAllRanks() {
        return rankRepository.findAll();
    }

    @Cacheable(CacheConfig.RANKS)
    public Optional<JsonObject> getRank(String uuid) {
        return rankRepository.findByUuid(uuid);
    }

    @CacheInvalidate(cacheNames = CacheConfig.RANKS, all = true)
    public JsonObject createRank(JsonObject rank) {
        JsonObject created = rankRepository.insert(rank);
        publish(new RankCreatePacket(UUID.fromString(rank.get("uuid").getAsString())));
        return created;
    }

    /** {@code PUT /api/rank} from {@code Rank.save()}: the UUID is in the body. */
    @CacheInvalidate(cacheNames = CacheConfig.RANKS, all = true)
    public JsonObject upsertRank(JsonObject rank) {
        JsonObject result = rankRepository.upsert(rank);
        publish(new RankUpdatePacket(UUID.fromString(rank.get("uuid").getAsString())));
        return result;
    }

    @CacheInvalidate(cacheNames = CacheConfig.RANKS, all = true)
    public boolean deleteRank(String uuid) {
        boolean deleted = rankRepository.deleteByUuid(uuid);
        if (deleted) publish(new RankDeletePacket(UUID.fromString(uuid)));
        return deleted;
    }

    private void publish(Packet packet) {
        try {
            redisService.publish(packet);
        } catch (Exception e) {
            log.warn("Could not publish {}: {}", packet.getClass().getSimpleName(), e.getMessage());
        }
    }
}
