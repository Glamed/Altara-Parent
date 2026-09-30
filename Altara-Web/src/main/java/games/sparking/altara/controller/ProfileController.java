package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.service.ProfileWebService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static games.sparking.altara.controller.Responses.*;

/**
 * Profile API used by Altara-Shared's {@code ProfileService}.
 *
 * <pre>
 *   GET    /api/profile/{uuid}               profile (always includes activeGrants)
 *   POST   /api/profile                      create
 *   PUT    /api/profile                      upsert (UUID in body)
 *   PUT    /api/profile/{uuid}               update
 *   GET    /api/profile/{uuid}/alts          accounts sharing an IP
 *   GET    /api/profile/{uuid}/grants        all grants
 *   POST   /api/profile/{uuid}/grants        add a grant
 *   PUT    /api/profile/{uuid}/grants/{id}   update/remove a grant
 *   POST   /api/profile/{uuid}/grants/clear  remove every active grant
 * </pre>
 */
@Controller("/api/profile")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileWebService profileWebService;

    @Get("/{uuid}")
    public HttpResponse<String> getProfile(UUID uuid) {
        return profileWebService.getProfile(uuid.toString())
                .map(Responses::ok)
                .orElseGet(() -> notFound("Profile not found: " + uuid));
    }

    @Post
    public HttpResponse<String> createProfile(@Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateUuid(json);
            return created(profileWebService.createProfile(json));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Put
    public HttpResponse<String> upsertProfile(@Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateUuid(json);
            return profileWebService.upsertProfile(json)
                    .map(Responses::ok)
                    .orElseGet(() -> serverError("Upsert failed"));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Put("/{uuid}")
    public HttpResponse<String> updateProfile(UUID uuid, @Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return profileWebService.updateProfile(uuid.toString(), json)
                    .map(Responses::ok)
                    .orElseGet(() -> notFound("Profile not found: " + uuid));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Get("/{uuid}/alts")
    public HttpResponse<String> getAlts(UUID uuid) {
        JsonArray array = new JsonArray();
        profileWebService.getAlts(uuid.toString()).forEach(array::add);
        return ok(array);
    }

    @Get("/{uuid}/grants")
    public HttpResponse<String> getGrants(UUID uuid) {
        return ok(profileWebService.getGrants(uuid.toString()));
    }

    @Post("/{uuid}/grants")
    public HttpResponse<String> addGrant(UUID uuid, @Body String body) {
        try {
            JsonObject grant = JsonParser.parseString(body).getAsJsonObject();
            if (!profileWebService.addGrant(uuid.toString(), grant)) return notFound("Profile not found: " + uuid);
            return ok("{\"success\":true}");
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Put("/{uuid}/grants/{grantId}")
    public HttpResponse<String> updateGrant(UUID uuid, String grantId, @Body String body) {
        try {
            JsonObject patch = JsonParser.parseString(body).getAsJsonObject();
            if (!profileWebService.updateGrant(uuid.toString(), grantId, patch)) return notFound("Grant not found: " + grantId);
            return ok("{\"success\":true}");
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    /** Optional body: {@code removedBy}, {@code removedAt}, {@code removedReason}. */
    @Post("/{uuid}/grants/clear")
    @Consumes(MediaType.ALL)
    public HttpResponse<String> clearGrants(UUID uuid, @Body @Nullable String body) {
        try {
            String removedBy = "Console";
            long removedAt = System.currentTimeMillis();
            String removedReason = "N/A";

            if (body != null && !body.isBlank()) {
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                if (json.has("removedBy")) removedBy = json.get("removedBy").getAsString();
                if (json.has("removedAt")) removedAt = json.get("removedAt").getAsLong();
                if (json.has("removedReason")) removedReason = json.get("removedReason").getAsString();
            }

            int removed = profileWebService.clearGrants(uuid.toString(), removedBy, removedAt, removedReason);
            return ok("{\"removed\":" + removed + "}");
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    /** The body must carry a well-formed {@code uuid}. */
    private static void validateUuid(JsonObject json) {
        if (!json.has("uuid") || json.get("uuid").isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: uuid");
        }
        UUID.fromString(json.get("uuid").getAsString());
    }
}
