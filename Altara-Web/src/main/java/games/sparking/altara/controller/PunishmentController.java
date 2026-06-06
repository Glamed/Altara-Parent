package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.service.PunishmentWebService;
import games.sparking.altara.utils.Statics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing the Punishment API.
 *
 * <pre>
 *   POST   /api/punishment                          — Issue a punishment (multi-action)
 *   GET    /api/punishment/{id}                     — Get by ID
 *   PATCH  /api/punishment/{id}                     — Partial update (infractionType / message / notes / actions)
 *   DELETE /api/punishment/{id}                     — Revoke (soft-delete)
 *   GET    /api/punishment/player/{uuid}            — All punishments for player
 *   GET    /api/punishment/player/{uuid}/active     — Active punishments for player
 *   GET    /api/punishment/player/{uuid}/banned     — Is player currently banned?
 *   GET    /api/punishment/infractions              — List all InfractionType values
 *   GET    /api/punishment/types                    — List all PunishmentType values
 * </pre>
 *
 * <h3>Issue request body</h3>
 * <pre>{@code
 * {
 *   "playerUuid":     "<uuid>",
 *   "staffUuid":      "<uuid>",          // optional – defaults to CONSOLE_UUID
 *   "infractionType": "PROFANITY",       // InfractionType enum name
 *   "actions": [
 *     { "type": "CHAT_RESTRICTION", "duration": 1800000 }
 *   ],
 *   "message": null,                     // optional chat message that triggered infraction
 *   "notes":   null                      // optional internal staff notes
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/punishment")
@RequiredArgsConstructor
public class PunishmentController {

    private final PunishmentWebService punishmentWebService;

    // ── Issue ──────────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> issuePunishment(@RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateIssueRequest(json);

            return punishmentWebService.issuePunishment(json)
                    .map(p -> ResponseEntity.status(201)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Statics.GSON.toJson(p)))
                    .orElse(ResponseEntity.internalServerError()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Failed to persist punishment\"}"));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPunishment(@PathVariable String id) {
        return punishmentWebService.getPunishment(id)
                .map(p -> ok(Statics.GSON.toJson(p)))
                .orElse(notFound("Punishment not found: " + id));
    }

    @GetMapping(value = "/player/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPlayerPunishments(@PathVariable UUID uuid) {
        JsonArray punishments = punishmentWebService.getPlayerPunishments(uuid);
        return ok(Statics.GSON.toJson(punishments));
    }

    @GetMapping(value = "/player/{uuid}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getActivePlayerPunishments(@PathVariable UUID uuid) {
        JsonArray punishments = punishmentWebService.getActivePlayerPunishments(uuid);
        return ok(Statics.GSON.toJson(punishments));
    }

    @GetMapping(value = "/player/{uuid}/banned", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> isPlayerBanned(@PathVariable UUID uuid) {
        boolean banned = punishmentWebService.isPlayerBanned(uuid);
        JsonObject result = new JsonObject();
        result.addProperty("uuid",   uuid.toString());
        result.addProperty("banned", banned);
        return ok(result.toString());
    }

    // ── Revoke ─────────────────────────────────────────────────────────────────

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> revokePunishment(
            @PathVariable String id,
            @RequestParam(required = false) String removedBy) {
        return punishmentWebService.revokePunishment(id, removedBy)
                .map(p -> ok(Statics.GSON.toJson(p)))
                .orElse(notFound("Punishment not found: " + id));
    }

    // ── Update (PATCH) ─────────────────────────────────────────────────────────

    /**
     * Partially updates a punishment.
     *
     * <h3>Accepted fields</h3>
     * <pre>{@code
     * {
     *   "infractionType": "SPAM",               // optional
     *   "message":        null,                  // optional (null clears it)
     *   "notes":          "Staff note",          // optional
     *   "actions": [                             // optional – replaces entire list
     *     { "type": "CHAT_RESTRICTION", "duration": 3600000 }
     *   ]
     * }
     * }</pre>
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updatePunishment(@PathVariable String id, @RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return punishmentWebService.updatePunishment(id, json)
                    .map(p -> ok(Statics.GSON.toJson(p)))
                    .orElse(notFound("Punishment not found: " + id));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Enum Metadata ──────────────────────────────────────────────────────────

    /**
     * Returns all non-hidden {@link InfractionType} values with their metadata.
     * Intended for populating web-panel dropdowns.
     */
    @GetMapping(value = "/infractions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listInfractions() {
        JsonArray arr = new JsonArray();
        for (InfractionType type : InfractionType.visibleValues()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name",        type.name());
            obj.addProperty("displayName", type.getDisplayName());
            obj.addProperty("description", type.getDescription());
            obj.addProperty("affirmation", type.getAffirmation());
            arr.add(obj);
        }
        return ok(Statics.GSON.toJson(arr));
    }

    /**
     * Returns all {@link PunishmentType} values with their display names.
     */
    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> listPunishmentTypes() {
        JsonArray arr = new JsonArray();
        for (PunishmentType type : PunishmentType.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name",        type.name());
            obj.addProperty("displayName", type.getName());
            arr.add(obj);
        }
        return ok(Statics.GSON.toJson(arr));
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private static void validateIssueRequest(JsonObject json) {
        requireField(json, "playerUuid");
        UUID.fromString(json.get("playerUuid").getAsString()); // format check
        requireField(json, "infractionType");
        InfractionType.valueOf(json.get("infractionType").getAsString()); // enum check
        if (!json.has("actions") || !json.get("actions").isJsonArray()
                || json.get("actions").getAsJsonArray().isEmpty()) {
            throw new IllegalArgumentException("'actions' must be a non-empty array");
        }
        for (var el : json.get("actions").getAsJsonArray()) {
            JsonObject a = el.getAsJsonObject();
            requireField(a, "type");
            PunishmentType.valueOf(a.get("type").getAsString()); // enum check
            requireField(a, "duration");
        }
    }

    private static void requireField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
    }

    // ── Response helpers ───────────────────────────────────────────────────────

    private ResponseEntity<String> ok(String body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    private ResponseEntity<String> notFound(String message) {
        return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"" + escape(message) + "\"}");
    }

    private ResponseEntity<String> badRequest(String message) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"" + escape(message) + "\"}");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}

