package games.sparking.altara.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for the {@code punishments} collection.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "_id":            "&lt;punishment-uuid&gt;",
 *   "id":             "&lt;punishment-uuid&gt;",
 *   "playerUuid":     "...",
 *   "staffUuid":      "...",
 *   "infractionType": "PROFANITY",
 *   "actions":        [ { "type": "CHAT_RESTRICTION", "duration": 1800000 } ],
 *   "message":        null,
 *   "notes":          null,
 *   "issuedAt":       1234567890000,
 *   "removed":        false,
 *   "removedAt":      -1,
 *   "removedBy":      null
 * }
 * </pre>
 */
@Repository
@RequiredArgsConstructor
public class PunishmentRepository {

    private static final String COLLECTION = "punishments";
    private static final JsonWriterSettings RELAXED =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final MongoTemplate mongoTemplate;

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public Optional<JsonObject> findById(String id) {
        Document doc = mongoTemplate.findOne(byId(id), Document.class, COLLECTION);
        return Optional.ofNullable(doc).map(this::toJson);
    }

    /**
     * Insert a new punishment document. The {@code _id} is set to the punishment's
     * own {@code id} field so queries by both {@code _id} and {@code id} work.
     */
    public JsonObject insert(JsonObject punishment) {
        Document doc = Document.parse(punishment.toString());
        doc.put("_id", punishment.get("id").getAsString());
        mongoTemplate.insert(doc, COLLECTION);
        return findById(punishment.get("id").getAsString()).orElse(punishment);
    }

    /**
     * Soft-delete a punishment (sets {@code removed=true}, {@code removedAt}, {@code removedBy}).
     */
    public Optional<JsonObject> revoke(String id, String removedBy) {
        Update update = new Update()
                .set("removed",   true)
                .set("removedAt", System.currentTimeMillis())
                .set("removedBy", removedBy);
        long matched = mongoTemplate.updateFirst(byId(id), update, COLLECTION).getMatchedCount();
        if (matched == 0) return Optional.empty();
        return findById(id);
    }

    /**
     * Partial update (PATCH) — only the allowed mutable fields are written.
     * Allowed: {@code infractionType}, {@code message}, {@code notes}, {@code actions}.
     */
    public Optional<JsonObject> patch(String id, JsonObject updates) {
        Update update = new Update();
        boolean any = false;

        if (updates.has("infractionType") && !updates.get("infractionType").isJsonNull()) {
            update.set("infractionType", updates.get("infractionType").getAsString());
            any = true;
        }
        if (updates.has("message")) {
            update.set("message", updates.get("message").isJsonNull() ? null : updates.get("message").getAsString());
            any = true;
        }
        if (updates.has("notes")) {
            update.set("notes", updates.get("notes").isJsonNull() ? null : updates.get("notes").getAsString());
            any = true;
        }
        if (updates.has("actions") && updates.get("actions").isJsonArray()) {
            update.set("actions", Document.parse("{\"v\":" + updates.get("actions").toString() + "}").get("v"));
            any = true;
        }

        if (!any) return findById(id);

        long matched = mongoTemplate.updateFirst(byId(id), update, COLLECTION).getMatchedCount();
        if (matched == 0) return Optional.empty();
        return findById(id);
    }

    // ── Player queries ─────────────────────────────────────────────────────────

    /** All punishment records for a player (newest first). */
    public JsonArray findByPlayer(String playerUuid) {
        Query query = Query.query(Criteria.where("playerUuid").is(playerUuid));
        List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION);
        return toArray(docs);
    }

    /** Only active punishments for a player. */
    public JsonArray findActiveByPlayer(String playerUuid) {
        Query query = Query.query(
                Criteria.where("playerUuid").is(playerUuid)
                        .and("removed").is(false)
        );
        List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION);

        // Filter client-side for expiry (duration is relative to issuedAt)
        JsonArray result = new JsonArray();
        for (Document d : docs) {
            JsonObject obj = toJson(d);
            if (isActive(obj)) result.add(obj);
        }
        return result;
    }

    /** Whether the player currently has an active SUSPENSION (ban). */
    public boolean isBanned(String playerUuid) {
        JsonArray actives = findActiveByPlayer(playerUuid);
        for (JsonElement el : actives) {
            JsonObject p = el.getAsJsonObject();
            if (hasSuspension(p)) return true;
        }
        return false;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Query byId(String id) {
        return Query.query(Criteria.where("id").is(id));
    }

    private JsonObject toJson(Document doc) {
        doc.remove("_id");
        JsonElement parsed = JsonParser.parseString(doc.toJson(RELAXED));
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }

    private JsonArray toArray(List<Document> docs) {
        JsonArray arr = new JsonArray();
        for (Document d : docs) arr.add(toJson(d));
        return arr;
    }

    /**
     * Determines activity purely from the JSON document — mirrors
     * {@code Punishment#isActive()} without needing the entity class here.
     */
    private static boolean isActive(JsonObject p) {
        if (p.has("removed") && p.get("removed").getAsBoolean()) return false;
        long issuedAt = p.has("issuedAt") ? p.get("issuedAt").getAsLong() : 0L;
        if (!p.has("actions") || !p.get("actions").isJsonArray()) return false;
        for (JsonElement el : p.get("actions").getAsJsonArray()) {
            JsonObject a = el.getAsJsonObject();
            long duration = a.has("duration") ? a.get("duration").getAsLong() : 0L;
            if (duration == -1L) return true;   // permanent
            if (duration == 0L)  continue;       // immediate
            if (System.currentTimeMillis() <= issuedAt + duration) return true;
        }
        return false;
    }

    private static boolean hasSuspension(JsonObject p) {
        if (!p.has("actions") || !p.get("actions").isJsonArray()) return false;
        for (JsonElement el : p.get("actions").getAsJsonArray()) {
            JsonObject a = el.getAsJsonObject();
            if ("SUSPENSION".equals(jsonString(a, "type"))) return true;
        }
        return false;
    }

    private static String jsonString(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : null;
    }
}

