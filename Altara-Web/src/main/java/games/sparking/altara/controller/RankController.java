package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.service.RankWebService;
import games.sparking.altara.utils.Statics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing the Rank API consumed by Altara-Shared's {@code RankService}.
 *
 * <pre>
 *   GET    /api/rank          — get all ranks (JsonArray)
 *   GET    /api/rank/{uuid}   — get single rank
 *   POST   /api/rank          — create rank
 *   PUT    /api/rank          — upsert rank  (Rank.save())
 *   DELETE /api/rank/{uuid}   — delete rank
 * </pre>
 */
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankWebService rankWebService;

    // ------------------------------------------------------------------
    // GET /api/rank
    // ------------------------------------------------------------------
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllRanks() {
        List<JsonObject> ranks = rankWebService.getAllRanks();
        JsonArray array = new JsonArray();
        ranks.forEach(array::add);
        return ok(array.toString());
    }

    // ------------------------------------------------------------------
    // GET /api/rank/{uuid}
    // ------------------------------------------------------------------
    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getRank(@PathVariable UUID uuid) {
        return rankWebService.getRank(uuid)
                .map(rank -> ok(Statics.GSON.toJson(rank)))
                .orElse(notFound("Rank not found: " + uuid));
    }

    // ------------------------------------------------------------------
    // POST /api/rank  — create
    // ------------------------------------------------------------------
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createRank(@RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("uuid") || json.get("uuid").isJsonNull()) {
                json.addProperty("uuid", UUID.randomUUID().toString());
            }
            JsonObject created = rankWebService.createRank(json);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON)
                    .body(Statics.GSON.toJson(created));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // PUT /api/rank  — upsert (from Rank.save())
    // ------------------------------------------------------------------
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> upsertRank(@RequestBody String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("uuid") || json.get("uuid").isJsonNull()) {
                return badRequest("Missing required field: uuid");
            }
            JsonObject result = rankWebService.upsertRank(json);
            return ok(Statics.GSON.toJson(result));
        } catch (Exception e) {
            return badRequest(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // DELETE /api/rank/{uuid}
    // ------------------------------------------------------------------
    @DeleteMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deleteRank(@PathVariable UUID uuid) {
        boolean deleted = rankWebService.deleteRank(uuid);
        if (!deleted) return notFound("Rank not found: " + uuid);
        return ok("{\"success\":true}");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
