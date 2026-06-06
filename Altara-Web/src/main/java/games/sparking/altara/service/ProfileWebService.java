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
        return profileRepository.findByUuid(uuid.toString())
                .map(profile -> {
                    // Guarantee that clients always receive an activeGrants array,
                    // even for profiles that pre-date this field being initialized on insert.
                    if (!profile.has("activeGrants") || profile.get("activeGrants").isJsonNull()) {
                        profile.add("activeGrants", new JsonArray());
                    }
                    // Normalize any grant whose "scopes" was stored as a legacy comma-string.
                    normalizeGrantScopes(profile.get("activeGrants").getAsJsonArray());
                    return profile;
                });
    }

    /**
     * Converts any grant entry whose {@code scopes} value is a comma-joined string
     * (legacy format) into a proper JSON array so clients can deserialize
     * {@code List<String>} without errors.
     */
    private void normalizeGrantScopes(JsonArray grants) {
        for (com.google.gson.JsonElement el : grants) {
            if (!el.isJsonObject()) continue;
            com.google.gson.JsonObject grant = el.getAsJsonObject();
            if (!grant.has("scopes")) continue;
            com.google.gson.JsonElement scopesEl = grant.get("scopes");
            if (scopesEl.isJsonPrimitive() && scopesEl.getAsJsonPrimitive().isString()) {
                String raw = scopesEl.getAsString();
                JsonArray arr = new JsonArray();
                for (String s : raw.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) arr.add(trimmed);
                }
                grant.add("scopes", arr);
            }
        }
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
        JsonArray grants = profileRepository.getGrants(uuid.toString());
        normalizeGrantScopes(grants);
        return grants;
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
    public int clearGrants(UUID uuid, String removedBy, long removedAt, String removedReason) {
        int count = profileRepository.clearGrants(uuid.toString(), removedBy, removedAt, removedReason);
        if (count > 0) publishProfileUpdate(uuid.toString());
        return count;
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
