package games.sparking.altara.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.config.CacheEvictor;
import games.sparking.altara.profile.packet.ProfileUpdatePacket;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.repository.ProfileRepository;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Profiles and their embedded grants.  Every write publishes a {@link ProfileUpdatePacket}. */
@Singleton
@RequiredArgsConstructor
@Slf4j
public class ProfileWebService {

    private final ProfileRepository profileRepository;
    private final RedisService redisService;
    private final CacheEvictor cacheEvictor;

    // ── Profiles ───────────────────────────────────────────────────────────────

    @Cacheable(CacheConfig.PROFILES)
    public Optional<JsonObject> getProfile(String uuid) {
        return profileRepository.findByUuid(uuid).map(profile -> {
            // Older profiles may lack activeGrants; clients always expect the array.
            if (!profile.has("activeGrants") || profile.get("activeGrants").isJsonNull()) {
                profile.add("activeGrants", new JsonArray());
            }
            normalizeGrantScopes(profile.get("activeGrants").getAsJsonArray());
            return profile;
        });
    }

    public JsonObject createProfile(JsonObject profile) {
        JsonObject created = profileRepository.insert(profile);
        afterWrite(profile.get("uuid").getAsString());
        return created;
    }

    /** {@code PUT /api/profile}: the UUID is in the body. */
    public Optional<JsonObject> upsertProfile(JsonObject profile) {
        String uuid = profile.get("uuid").getAsString();
        Optional<JsonObject> result = profileRepository.upsert(profile);
        if (result.isPresent()) afterWrite(uuid);
        return result;
    }

    public Optional<JsonObject> updateProfile(String uuid, JsonObject profile) {
        Optional<JsonObject> result = profileRepository.update(uuid, profile);
        if (result.isPresent()) afterWrite(uuid);
        return result;
    }

    @Cacheable(CacheConfig.PROFILE_ALTS)
    public List<JsonObject> getAlts(String uuid) {
        return profileRepository.findAlts(uuid);
    }

    // ── Grants ─────────────────────────────────────────────────────────────────

    @Cacheable(CacheConfig.PROFILE_GRANTS)
    public JsonArray getGrants(String uuid) {
        JsonArray grants = profileRepository.getGrants(uuid);
        normalizeGrantScopes(grants);
        return grants;
    }

    @CacheInvalidate(cacheNames = {CacheConfig.PROFILES, CacheConfig.PROFILE_GRANTS}, parameters = "uuid")
    public boolean addGrant(String uuid, JsonObject grant) {
        boolean ok = profileRepository.addGrant(uuid, grant);
        if (ok) publishProfileUpdate(uuid);
        return ok;
    }

    @CacheInvalidate(cacheNames = {CacheConfig.PROFILES, CacheConfig.PROFILE_GRANTS}, parameters = "uuid")
    public boolean updateGrant(String uuid, String grantId, JsonObject patch) {
        boolean ok = profileRepository.updateGrant(uuid, grantId, patch);
        if (ok) publishProfileUpdate(uuid);
        return ok;
    }

    @CacheInvalidate(cacheNames = {CacheConfig.PROFILES, CacheConfig.PROFILE_GRANTS}, parameters = "uuid")
    public int clearGrants(String uuid, String removedBy, long removedAt, String removedReason) {
        int count = profileRepository.clearGrants(uuid, removedBy, removedAt, removedReason);
        if (count > 0) publishProfileUpdate(uuid);
        return count;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void afterWrite(String uuid) {
        cacheEvictor.profile(uuid);
        publishProfileUpdate(uuid);
    }

    /** Grants once stored {@code scopes} as a comma-joined string; clients expect an array. */
    private static void normalizeGrantScopes(JsonArray grants) {
        for (JsonElement element : grants) {
            if (!element.isJsonObject()) continue;
            JsonObject grant = element.getAsJsonObject();
            JsonElement scopes = grant.get("scopes");
            if (scopes == null || !scopes.isJsonPrimitive() || !scopes.getAsJsonPrimitive().isString()) continue;

            JsonArray array = new JsonArray();
            for (String scope : scopes.getAsString().split(",")) {
                if (!scope.isBlank()) array.add(scope.trim());
            }
            grant.add("scopes", array);
        }
    }

    private void publishProfileUpdate(String uuid) {
        try {
            redisService.publish(new ProfileUpdatePacket(UUID.fromString(uuid)));
        } catch (Exception e) {
            log.warn("Could not publish ProfileUpdatePacket for {}: {}", uuid, e.getMessage());
        }
    }
}
