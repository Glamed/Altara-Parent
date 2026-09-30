package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.service.PunishmentWebService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static games.sparking.altara.controller.Responses.*;

/**
 * Punishment API.
 *
 * <pre>
 *   POST   /api/punishment                        issue (multi-action)
 *   GET    /api/punishment/{id}                   one punishment
 *   PATCH  /api/punishment/{id}                   edit infractionType / message / notes / actions
 *   DELETE /api/punishment/{id}?removedBy=        revoke (soft-delete)
 *   POST   /api/punishment/{id}/notified          the player has been shown it
 *   GET    /api/punishment/player/{uuid}          every punishment for a player
 *   GET    /api/punishment/player/{uuid}/active   active punishments
 *   GET    /api/punishment/player/{uuid}/banned   {"uuid", "banned"}
 *   GET    /api/punishment/infractions            visible InfractionType values
 *   GET    /api/punishment/types                  PunishmentType values
 * </pre>
 *
 * Issue body:
 * <pre>{@code
 * {
 *   "playerUuid": "<uuid>", "staffUuid": "<uuid or null for console>",
 *   "infractionType": "PROFANITY",
 *   "actions": [ { "type": "CHAT_RESTRICTION", "duration": 1800000 } ],
 *   "message": null, "notes": null
 * }
 * }</pre>
 */
@Controller("/api/punishment")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class PunishmentController {

    private final PunishmentWebService punishmentWebService;

    // ── Issue ──────────────────────────────────────────────────────────────────

    @Post
    public HttpResponse<String> issuePunishment(@Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateIssueRequest(json);
            return punishmentWebService.issuePunishment(json)
                    .map(Responses::created)
                    .orElseGet(() -> serverError("Failed to persist punishment"));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    @Get("/{id}")
    public HttpResponse<String> getPunishment(String id) {
        return punishmentWebService.getPunishment(id)
                .map(Responses::ok)
                .orElseGet(() -> notFound("Punishment not found: " + id));
    }

    @Get("/player/{uuid}")
    public HttpResponse<String> getPlayerPunishments(UUID uuid) {
        return ok(punishmentWebService.getPlayerPunishments(uuid.toString()));
    }

    @Get("/player/{uuid}/active")
    public HttpResponse<String> getActivePlayerPunishments(UUID uuid) {
        return ok(punishmentWebService.getActivePlayerPunishments(uuid.toString()));
    }

    @Get("/player/{uuid}/banned")
    public HttpResponse<String> isPlayerBanned(UUID uuid) {
        JsonObject result = new JsonObject();
        result.addProperty("uuid", uuid.toString());
        result.addProperty("banned", punishmentWebService.isPlayerBanned(uuid.toString()));
        return ok(result.toString());
    }

    // ── Revoke / notify / edit ─────────────────────────────────────────────────

    @Delete("/{id}")
    public HttpResponse<String> revokePunishment(String id, @QueryValue @Nullable String removedBy) {
        return punishmentWebService.revokePunishment(id, removedBy)
                .map(Responses::ok)
                .orElseGet(() -> notFound("Punishment not found: " + id));
    }

    /** Called by a game server once the player has actually seen the punishment. */
    @Post("/{id}/notified")
    @Consumes(MediaType.ALL)
    public HttpResponse<String> markNotified(String id) {
        return punishmentWebService.markNotified(id)
                .map(Responses::ok)
                .orElseGet(() -> notFound("Punishment not found: " + id));
    }

    /** Body may set {@code infractionType}, {@code message} (null clears), {@code notes}, {@code actions} (replaces). */
    @Patch("/{id}")
    public HttpResponse<String> updatePunishment(String id, @Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return punishmentWebService.updatePunishment(id, json)
                    .map(Responses::ok)
                    .orElseGet(() -> notFound("Punishment not found: " + id));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Enum metadata (web panel dropdowns) ────────────────────────────────────

    @Get("/infractions")
    public HttpResponse<String> listInfractions() {
        JsonArray array = new JsonArray();
        for (InfractionType type : InfractionType.visibleValues()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", type.name());
            obj.addProperty("displayName", type.getDisplayName());
            obj.addProperty("description", type.getDescription());
            obj.addProperty("affirmation", type.getAffirmation());
            array.add(obj);
        }
        return ok(array);
    }

    @Get("/types")
    public HttpResponse<String> listPunishmentTypes() {
        JsonArray array = new JsonArray();
        for (PunishmentType type : PunishmentType.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", type.name());
            obj.addProperty("displayName", type.getName());
            array.add(obj);
        }
        return ok(array);
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private static void validateIssueRequest(JsonObject json) {
        requireField(json, "playerUuid");
        UUID.fromString(json.get("playerUuid").getAsString());
        requireField(json, "infractionType");
        InfractionType.valueOf(json.get("infractionType").getAsString());

        if (!json.has("actions") || !json.get("actions").isJsonArray() || json.get("actions").getAsJsonArray().isEmpty()) {
            throw new IllegalArgumentException("'actions' must be a non-empty array");
        }
        for (JsonElement element : json.get("actions").getAsJsonArray()) {
            JsonObject action = element.getAsJsonObject();
            requireField(action, "type");
            PunishmentType.valueOf(action.get("type").getAsString());
            requireField(action, "duration");
        }
    }

    private static void requireField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
    }
}
