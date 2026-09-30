package games.sparking.altara.service;

import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.repository.ProfileRepository;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * UUID ↔ name lookups backed by the {@code profiles} collection.  {@link ProfileWebService}
 * clears these caches whenever a profile is written, so name changes show up promptly.
 */
@Singleton
@RequiredArgsConstructor
public class UUIDWebService {

    private final ProfileRepository profileRepository;

    /**
     * {@code {"uuid": ..., "name": ...}}.  Callers pass the name lower-cased (the match is
     * case-insensitive) so every spelling shares one cache entry.
     */
    @Cacheable(CacheConfig.UUID_BY_NAME)
    public Optional<String> resolveNameToJson(String lowerCaseName) {
        return profileRepository.findByName(lowerCaseName).map(UUIDWebService::toJson);
    }

    @Cacheable(CacheConfig.UUID_BY_UUID)
    public Optional<String> resolveUuidToJson(String uuid) {
        return profileRepository.findByUuid(uuid).map(UUIDWebService::toJson);
    }

    private static String toJson(JsonObject profile) {
        JsonObject result = new JsonObject();
        if (profile.has("uuid")) result.add("uuid", profile.get("uuid"));
        if (profile.has("name")) result.add("name", profile.get("name"));
        return result.toString();
    }
}
