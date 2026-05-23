package games.sparking.altara.service;

import com.google.gson.JsonObject;
import games.sparking.altara.config.CacheConfig;
import games.sparking.altara.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for UUID ↔ name lookups backed by the "profiles" MongoDB collection.
 *
 * <p>Results are cached with a 30-minute TTL via Caffeine (see {@link CacheConfig}).
 * Cache entries are automatically evicted by {@link ProfileWebService} whenever a
 * profile is created, upserted, or updated — ensuring name changes are reflected
 * promptly.</p>
 */
@Service
@RequiredArgsConstructor
public class UUIDWebService {

    private final ProfileRepository profileRepository;

    /**
     * Resolve a player name to a JSON snippet containing {@code uuid} and {@code name}.
     * Cached by lower-cased name so look-ups are case-insensitive without duplicating entries.
     */
    @Cacheable(value = CacheConfig.UUID_BY_NAME, key = "#name.toLowerCase()")
    public Optional<String> resolveNameToJson(String name) {
        return profileRepository.findByName(name)
                .map(this::toJson);
    }

    /**
     * Resolve a UUID to a JSON snippet containing {@code uuid} and {@code name}.
     */
    @Cacheable(value = CacheConfig.UUID_BY_UUID, key = "#uuid.toString()")
    public Optional<String> resolveUuidToJson(UUID uuid) {
        return profileRepository.findByUuid(uuid.toString())
                .map(this::toJson);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String toJson(JsonObject profile) {
        JsonObject result = new JsonObject();
        if (profile.has("uuid")) result.add("uuid", profile.get("uuid"));
        if (profile.has("name")) result.add("name", profile.get("name"));
        return result.toString();
    }
}
