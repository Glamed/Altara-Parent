package games.sparking.altara.repository;

import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Ranks in the {@code ranks} collection; {@code _id} is the rank UUID. */
@Singleton
public class RankRepository {

    private final MongoCollection<Document> collection;

    public RankRepository(MongoDatabase database) {
        this.collection = database.getCollection("ranks");
    }

    public List<JsonObject> findAll() {
        List<JsonObject> result = new ArrayList<>();
        for (Document doc : collection.find()) result.add(MongoJson.toJson(doc));
        return result;
    }

    public Optional<JsonObject> findByUuid(String uuid) {
        return Optional.ofNullable(collection.find(byUuid(uuid)).first()).map(MongoJson::toJson);
    }

    public JsonObject insert(JsonObject rank) {
        String uuid = rank.get("uuid").getAsString();
        Document doc = MongoJson.toDocument(rank);
        doc.put("_id", uuid);
        collection.insertOne(doc);
        return findByUuid(uuid).orElse(rank);
    }

    public JsonObject upsert(JsonObject rank) {
        String uuid = rank.get("uuid").getAsString();
        Document doc = MongoJson.toDocument(rank);
        doc.put("_id", uuid);
        collection.replaceOne(Filters.eq("_id", uuid), doc, MongoJson.UPSERT);
        return findByUuid(uuid).orElse(rank);
    }

    public boolean deleteByUuid(String uuid) {
        return collection.deleteMany(byUuid(uuid)).getDeletedCount() > 0;
    }

    private static Bson byUuid(String uuid) {
        return Filters.eq("uuid", uuid);
    }
}
