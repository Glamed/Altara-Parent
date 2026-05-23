package games.sparking.altara.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.profile.packet.ProfileUpdatePacket;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileWebService {

    private final ProfileRepository profileRepository;
    private final RedisService redisService;

    // ------------------------------------------------------------------
    // Profile CRUD
    // ------------------------------------------------------------------

    @Cacheable(value = CacheConfig.PROFILES, key = "#uuid.toString()")
    public Optional<JsonObject> getProfile(UUID uuid) {
        return profileRepository.findByUuid(uuid.toString());
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PROFILES,       key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.PROFILE_GRANTS, key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.PROFILE_ALTS,   key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.UUID_BY_UUID,   key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.UUID_BY_NAME,   allEntries = true)
    })
    public JsonObject createProfile(JsonObject profile) {
        JsonObject created = profileRepository.insert(profile);
        publishProfileUpdate(profile.get("uuid").getAsString());
        return created;
    }

    /**
     * Upsert — used by {@code Profile.save()} which sends {@code PUT /api/profile} (no UUID in URL).
     */
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PROFILES,       key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.PROFILE_GRANTS, key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.PROFILE_ALTS,   key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.UUID_BY_UUID,   key = "#profile.get('uuid').getAsString()"),
            @CacheEvict(value = CacheConfig.UUID_BY_NAME,   allEntries = true)
    })
    public Optional<JsonObject> upsertProfile(JsonObject profile) {
        Optional<JsonObject> result = profileRepository.upsert(profile);
        result.ifPresent(p -> publishProfileUpdate(p.get("uuid").getAsString()));
        return result;
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PROFILES,       key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.PROFILE_GRANTS, key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.PROFILE_ALTS,   key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.UUID_BY_UUID,   key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.UUID_BY_NAME,   allEntries = true)
    })
    public Optional<JsonObject> updateProfile(UUID uuid, JsonObject profile) {
        Optional<JsonObject> result = profileRepository.update(uuid.toString(), profile);
        result.ifPresent(p -> publishProfileUpdate(uuid.toString()));
        return result;
    }

    @Cacheable(value = CacheConfig.PROFILE_ALTS, key = "#uuid.toString()")
    public List<JsonObject> getAlts(UUID uuid) {
        return profileRepository.findAlts(uuid.toString());
    }

    // ------------------------------------------------------------------
    // Grants
    // ------------------------------------------------------------------

    @Cacheable(value = CacheConfig.PROFILE_GRANTS, key = "#uuid.toString()")
    public JsonArray getGrants(UUID uuid) {
        return profileRepository.getGrants(uuid.toString());
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PROFILES,       key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.PROFILE_GRANTS, key = "#uuid.toString()")
    })
    public boolean addGrant(UUID uuid, JsonObject grant) {
        boolean ok = profileRepository.addGrant(uuid.toString(), grant);
        if (ok) publishProfileUpdate(uuid.toString());
        return ok;
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PROFILES,       key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.PROFILE_GRANTS, key = "#uuid.toString()")
    })
    public boolean updateGrant(UUID uuid, String grantId, JsonObject patch) {
        boolean ok = profileRepository.updateGrant(uuid.toString(), grantId, patch);
        if (ok) publishProfileUpdate(uuid.toString());
        return ok;
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PROFILES,       key = "#uuid.toString()"),
            @CacheEvict(value = CacheConfig.PROFILE_GRANTS, key = "#uuid.toString()")
    })
    public boolean clearGrants(UUID uuid) {
        boolean ok = profileRepository.clearGrants(uuid.toString());
        if (ok) publishProfileUpdate(uuid.toString());
        return ok;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void publishProfileUpdate(String uuidStr) {
        try {
            redisService.publish(new ProfileUpdatePacket(UUID.fromString(uuidStr)));
        } catch (Exception e) {
            log.warn("Could not publish ProfileUpdatePacket for {}: {}", uuidStr, e.getMessage());
        }
    }
}
