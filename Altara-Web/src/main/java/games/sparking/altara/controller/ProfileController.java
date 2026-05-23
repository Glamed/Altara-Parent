package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.service.ProfileWebService;
import games.sparking.altara.utils.Statics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the Profile API consumed by Altara-Shared's {@code ProfileService}.
 *
 * <pre>
 *   GET    /api/profile/{uuid}               — get profile (includes activeGrants)
 *   POST   /api/profile                      — create profile
 *   PUT    /api/profile                      — upsert profile  (Profile.save())
 *   PUT    /api/profile/{uuid}               — update profile  (ProfileService.updateProfile())
 *   GET    /api/profile/{uuid}/alts          — get alt accounts
 *   GET    /api/profile/{uuid}/grants        — get all grants
 *   POST   /api/profile/{uuid}/grants        — add grant
 *   PUT    /api/profile/{uuid}/grants/{id}   — update/remove grant
 *   POST   /api/profile/{uuid}/grants/clear  — clear all grants
 * </pre>
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileWebService profileWebService;

    // ------------------------------------------------------------------
    // GET /api/profile/{uuid}
    // ------------------------------------------------------------------
    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getProfile(@PathVariable UUID uuid) {
        return profileWebService.getProfile(uuid)
                .map(this::ok)
                .orElse(notFound("Profile not found: " + uuid));
    }

    // ------------------------------------------------------------------
    // POST /api/profile  — create
    // ------------------------------------------------------------------
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createProfile(@RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateUuid(json);
            JsonObject created = profileWebService.createProfile(json);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON)
                    .body(Statics.GSON.toJson(created));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // PUT /api/profile  — upsert (from Profile.save())
    // ------------------------------------------------------------------
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> upsertProfile(@RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateUuid(json);
            return profileWebService.upsertProfile(json)
                    .map(this::ok)
                    .orElse(ResponseEntity.internalServerError()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Upsert failed\"}"));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // PUT /api/profile/{uuid}  — update (from ProfileService.updateProfile())
    // ------------------------------------------------------------------
    @PutMapping(value = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateProfile(@PathVariable UUID uuid, @RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return profileWebService.updateProfile(uuid, json)
                    .map(this::ok)
                    .orElse(notFound("Profile not found: " + uuid));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // GET /api/profile/{uuid}/alts
    // ------------------------------------------------------------------
    @GetMapping(value = "/{uuid}/alts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAlts(@PathVariable UUID uuid) {
        List<JsonObject> alts = profileWebService.getAlts(uuid);
        JsonArray array = new JsonArray();
        alts.forEach(array::add);
        return ok(array.toString());
    }

    // ------------------------------------------------------------------
    // GET /api/profile/{uuid}/grants
    // ------------------------------------------------------------------
    @GetMapping(value = "/{uuid}/grants", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getGrants(@PathVariable UUID uuid) {
        JsonArray grants = profileWebService.getGrants(uuid);
        return ok(grants.toString());
    }

    // ------------------------------------------------------------------
    // POST /api/profile/{uuid}/grants  — add grant
    // ------------------------------------------------------------------
    @PostMapping(value = "/{uuid}/grants", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addGrant(@PathVariable UUID uuid, @RequestBody String body) {
        try {
            JsonObject grant = JsonParser.parseString(body).getAsJsonObject();
            boolean ok = profileWebService.addGrant(uuid, grant);
            if (!ok) return notFound("Profile not found: " + uuid);
            return ok("{\"success\":true}");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // PUT /api/profile/{uuid}/grants/{grantId}  — update/remove grant
    // ------------------------------------------------------------------
    @PutMapping(value = "/{uuid}/grants/{grantId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateGrant(@PathVariable UUID uuid,
                                              @PathVariable String grantId,
                                              @RequestBody String body) {
        try {
            JsonObject patch = JsonParser.parseString(body).getAsJsonObject();
            boolean ok = profileWebService.updateGrant(uuid, grantId, patch);
            if (!ok) return notFound("Grant not found: " + grantId);
            return ok("{\"success\":true}");
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // POST /api/profile/{uuid}/grants/clear  — clear all grants
    // ------------------------------------------------------------------
    @PostMapping(value = "/{uuid}/grants/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> clearGrants(@PathVariable UUID uuid) {
        boolean ok = profileWebService.clearGrants(uuid);
        if (!ok) return notFound("Profile not found: " + uuid);
        return ok("{\"success\":true}");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ResponseEntity<String> ok(JsonObject json) {
        return ok(Statics.GSON.toJson(json));
    }

    private ResponseEntity<String> ok(String json) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

    private ResponseEntity<String> notFound(String message) {
        return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"" + message + "\"}");
    }

    private ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"" + escape(message) + "\"}");
    }

    /** Validates that the JSON body contains a parseable "uuid" field. */
    private static void validateUuid(JsonObject json) {
        if (!json.has("uuid") || json.get("uuid").isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: uuid");
        }
        // throws IllegalArgumentException if malformed — result intentionally discarded
        @SuppressWarnings("unused")
        UUID ignored = UUID.fromString(json.get("uuid").getAsString());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
