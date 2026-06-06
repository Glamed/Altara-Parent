package games.sparking.altara.repository;

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
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ranks stored in the "ranks" MongoDB collection.
 */
@Repository
@RequiredArgsConstructor
public class RankRepository {

    private static final String COLLECTION = "ranks";
    private static final JsonWriterSettings RELAXED =
            JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final MongoTemplate mongoTemplate;

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    public List<JsonObject> findAll() {
        List<Document> docs = mongoTemplate.findAll(Document.class, COLLECTION);
        List<JsonObject> result = new ArrayList<>();
        for (Document doc : docs) result.add(toJson(doc));
        return result;
    }

    public Optional<JsonObject> findByUuid(String uuid) {
        Document doc = mongoTemplate.findOne(byUuid(uuid), Document.class, COLLECTION);
        return Optional.ofNullable(doc).map(this::toJson);
    }

    /**
     * Insert a new rank.  The {@code _id} is set to the rank UUID string.
     */
    public JsonObject insert(JsonObject rank) {
        Document doc = Document.parse(rank.toString());
        doc.put("_id", rank.get("uuid").getAsString());
        mongoTemplate.insert(doc, COLLECTION);
        return findByUuid(rank.get("uuid").getAsString()).orElse(rank);
    }

    /**
     * Upsert a rank by its UUID (contained in the body).
     */
    public JsonObject upsert(JsonObject rank) {
        String uuid = rank.get("uuid").getAsString();
        Document doc = Document.parse(rank.toString());
        doc.put("_id", uuid);
        mongoTemplate.save(doc, COLLECTION);
        return findByUuid(uuid).orElse(rank);
    }

    public boolean deleteByUuid(String uuid) {
        return mongoTemplate.remove(byUuid(uuid), COLLECTION).getDeletedCount() > 0;
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
