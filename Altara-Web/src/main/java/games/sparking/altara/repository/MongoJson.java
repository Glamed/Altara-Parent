package games.sparking.altara.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

/** Conversions between Mongo documents and the Gson JSON the API speaks. */
final class MongoJson {

    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    /** {@code replaceOne} that inserts when the document doesn't exist yet. */
    static final ReplaceOptions UPSERT = new ReplaceOptions().upsert(true);

    private MongoJson() {}

    static Document toDocument(JsonElement json) {
        return Document.parse(json.toString());
    }

    /** The document as JSON, without Mongo's internal {@code _id}. */
    static JsonObject toJson(Document doc) {
        doc.remove("_id");
        JsonElement parsed = JsonParser.parseString(doc.toJson(RELAXED));
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }

    static JsonArray toArray(Iterable<Document> docs) {
        JsonArray array = new JsonArray();
        for (Document doc : docs) array.add(toJson(doc));
        return array;
    }

    static String string(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
