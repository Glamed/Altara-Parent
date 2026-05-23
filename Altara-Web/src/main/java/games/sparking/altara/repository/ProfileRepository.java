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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for player profiles stored in the "profiles" MongoDB collection.
 * Grants are embedded as an {@code activeGrants} array within each profile document.
 */
@Repository
@RequiredArgsConstructor
public class ProfileRepository {

    private static final String COLLECTION = "profiles";
    private static final JsonWriterSettings RELAXED =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final MongoTemplate mongoTemplate;

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    public Optional<JsonObject> findByUuid(String uuid) {
        Document doc = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        return Optional.ofNullable(doc).map(this::toJson);
    }

    /**
     * Find a profile by case-insensitive player name.
     */
    public Optional<JsonObject> findByName(String name) {
        Query query = Query.query(Criteria.where("name").regex("^" + java.util.regex.Pattern.quote(name) + "$", "i"));
        Document doc = mongoTemplate.findOne(query, Document.class, COLLECTION);
        return Optional.ofNullable(doc).map(this::toJson);
    }

    /**
     * Insert a new profile.  The {@code _id} is set to the profile UUID string.
     */
    public JsonObject insert(JsonObject profile) {
        Document doc = Document.parse(profile.toString());
        doc.put("_id", profile.get("uuid").getAsString());
        mongoTemplate.insert(doc, COLLECTION);
        return findByUuid(profile.get("uuid").getAsString()).orElse(profile);
    }

    /**
     * Upsert a profile.  Grants are excluded from the update (they are managed separately).
     */
    public Optional<JsonObject> upsert(JsonObject profile) {
        String uuid = profile.get("uuid").getAsString();

        // Build update document from the incoming JSON, excluding activeGrants
        Document update = Document.parse(profile.toString());
        update.remove("activeGrants");
        update.remove("_id");

        // Load existing or create a fresh skeleton
        Document existing = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        Document full = (existing != null) ? existing : new Document("_id", uuid).append("activeGrants", List.of());

        // Merge non-grants fields without a lambda
        for (Map.Entry<String, Object> entry : update.entrySet()) {
            if (!entry.getKey().equals("_id")) {
                full.put(entry.getKey(), entry.getValue());
            }
        }

        mongoTemplate.save(full, COLLECTION);

        return findByUuid(uuid);
    }

    public Optional<JsonObject> update(String uuid, JsonObject profile) {
        Document update = Document.parse(profile.toString());
        update.remove("activeGrants");
        update.remove("_id");

        Document existing = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        if (existing == null) return Optional.empty();

        update.forEach((k, v) -> { if (!k.equals("_id")) existing.put(k, v); });
        mongoTemplate.save(existing, COLLECTION);

        return findByUuid(uuid);
    }

    // ------------------------------------------------------------------
    // Alts (profiles sharing a known IP with the given profile)
    // ------------------------------------------------------------------

    public List<JsonObject> findAlts(String uuid) {
        Document profile = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        if (profile == null) return List.of();

        @SuppressWarnings("unchecked")
        List<String> knownIps = (List<String>) profile.getOrDefault("knownIps", List.of());
        if (knownIps.isEmpty()) return List.of();

        Query query = Query.query(
                Criteria.where("uuid").ne(uuid)
                        .and("knownIps").in(knownIps)
        );
        List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION);
        List<JsonObject> alts = new ArrayList<>();
        for (Document doc : docs) alts.add(toJson(doc));
        return alts;
    }

    // ------------------------------------------------------------------
    // Grants — embedded in the profile document
    // ------------------------------------------------------------------

    public JsonArray getGrants(String uuid) {
        Document profile = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        if (profile == null) return new JsonArray();
        Object grantsList = profile.get("activeGrants");
        if (!(grantsList instanceof List)) return new JsonArray();

        JsonArray array = new JsonArray();
        for (Object g : (List<?>) grantsList) {
            if (g instanceof Document grantDoc) {
                array.add(JsonParser.parseString(grantDoc.toJson(RELAXED)));
            }
        }
        return array;
    }

    /**
     * Append a grant document to the profile's {@code activeGrants} array.
     */
    public boolean addGrant(String uuid, JsonObject grant) {
        Document grantDoc = Document.parse(grant.toString());
        Update update = new Update().push("activeGrants", grantDoc);
        return mongoTemplate.updateFirst(byUuid(uuid), update, COLLECTION).getMatchedCount() > 0;
    }

    /**
     * Update (patch) an existing grant inside the {@code activeGrants} array.
     */
    public boolean updateGrant(String uuid, String grantId, JsonObject patch) {
        Document profile = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        if (profile == null) return false;

        @SuppressWarnings("unchecked")
        List<Document> grants = (List<Document>) profile.getOrDefault("activeGrants", new ArrayList<>());
        boolean found = false;
        for (Document g : grants) {
            if (grantId.equals(g.getString("id"))) {
                patch.entrySet().forEach(e -> g.put(e.getKey(), e.getValue().isJsonPrimitive()
                        ? (e.getValue().getAsJsonPrimitive().isBoolean()
                            ? e.getValue().getAsBoolean()
                            : e.getValue().isJsonNull() ? null : e.getValue().getAsString())
                        : e.getValue().toString()));
                // For numeric values, re-parse properly
                if (patch.has("removedAt")) g.put("removedAt", patch.get("removedAt").getAsLong());
                if (patch.has("removed"))   g.put("removed",   patch.get("removed").getAsBoolean());
                found = true;
                break;
            }
        }
        if (!found) return false;

        profile.put("activeGrants", grants);
        mongoTemplate.save(profile, COLLECTION);
        return true;
    }

    /**
     * Clear all grants from a profile.
     */
    public boolean clearGrants(String uuid) {
        Update update = new Update().set("activeGrants", List.of());
        return mongoTemplate.updateFirst(byUuid(uuid), update, COLLECTION).getMatchedCount() > 0;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Query byUuid(String uuid) {
        return Query.query(Criteria.where("uuid").is(uuid));
    }

    private JsonObject toJson(Document doc) {
        doc.remove("_id");
        String json = doc.toJson(RELAXED);
        JsonElement parsed = JsonParser.parseString(json);
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }
}
