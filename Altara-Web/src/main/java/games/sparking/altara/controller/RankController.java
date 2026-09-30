package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.service.RankWebService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static games.sparking.altara.controller.Responses.*;

/**
 * Rank API used by Altara-Shared's {@code RankService}.
 *
 * <pre>
 *   GET    /api/rank          all ranks
 *   GET    /api/rank/{uuid}   one rank
 *   POST   /api/rank          create (a UUID is generated if missing)
 *   PUT    /api/rank          upsert (UUID in body)
 *   DELETE /api/rank/{uuid}   delete
 * </pre>
 */
@Controller("/api/rank")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class RankController {

    private final RankWebService rankWebService;

    @Get
    public HttpResponse<String> getAllRanks() {
        JsonArray array = new JsonArray();
        rankWebService.getAllRanks().forEach(array::add);
        return ok(array);
    }

    @Get("/{uuid}")
    public HttpResponse<String> getRank(UUID uuid) {
        return rankWebService.getRank(uuid.toString())
                .map(Responses::ok)
                .orElseGet(() -> notFound("Rank not found: " + uuid));
    }

    @Post
    public HttpResponse<String> createRank(@Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("uuid") || json.get("uuid").isJsonNull()) {
                json.addProperty("uuid", UUID.randomUUID().toString());
            }
            return created(rankWebService.createRank(json));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Put
    public HttpResponse<String> upsertRank(@Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("uuid") || json.get("uuid").isJsonNull()) return badRequest("Missing required field: uuid");
            return ok(rankWebService.upsertRank(json));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Delete("/{uuid}")
    public HttpResponse<String> deleteRank(UUID uuid) {
        if (!rankWebService.deleteRank(uuid.toString())) return notFound("Rank not found: " + uuid);
        return ok("{\"success\":true}");
    }
}
