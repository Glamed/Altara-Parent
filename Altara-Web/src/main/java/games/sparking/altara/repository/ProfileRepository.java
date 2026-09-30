package games.sparking.altara.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Player profiles in the {@code profiles} collection; {@code _id} is the player UUID.
 * Grants are embedded as the {@code activeGrants} array and managed only through the grant methods.
 */
@Singleton
public class ProfileRepository {

    private final MongoCollection<Document> collection;

    public ProfileRepository(MongoDatabase database) {
        this.collection = database.getCollection("profiles");
    }

    // ── Profiles ───────────────────────────────────────────────────────────────

    public Optional<JsonObject> findByUuid(String uuid) {
        return Optional.ofNullable(findDocument(uuid)).map(MongoJson::toJson);
    }

    /** Case-insensitive exact name match. */
    public Optional<JsonObject> findByName(String name) {
        Bson filter = Filters.regex("name", "^" + Pattern.quote(name) + "$", "i");
        return Optional.ofNullable(collection.find(filter).first()).map(MongoJson::toJson);
    }

    /** Inserts a new profile; {@code activeGrants} always exists so clients can iterate it. */
    public JsonObject insert(JsonObject profile) {
        String uuid = profile.get("uuid").getAsString();
        Document doc = MongoJson.toDocument(profile);
        doc.put("_id", uuid);
        doc.putIfAbsent("activeGrants", List.of());
        collection.insertOne(doc);
        return findByUuid(uuid).orElse(profile);
    }

    /** Creates or updates a profile.  Grants in the body are ignored; they're managed separately. */
    public Optional<JsonObject> upsert(JsonObject profile) {
        String uuid = profile.get("uuid").getAsString();
        Document existing = findDocument(uuid);
        Document full = existing != null ? existing : new Document("_id", uuid).append("activeGrants", List.of());
        mergeWithoutGrants(full, profile);
        collection.replaceOne(Filters.eq("_id", full.get("_id")), full, MongoJson.UPSERT);
        return findByUuid(uuid);
    }

    /** Updates an existing profile; empty if there isn't one.  Grants in the body are ignored. */
    public Optional<JsonObject> update(String uuid, JsonObject profile) {
        Document existing = findDocument(uuid);
        if (existing == null) return Optional.empty();
        mergeWithoutGrants(existing, profile);
        collection.replaceOne(Filters.eq("_id", existing.get("_id")), existing);
        return findByUuid(uuid);
    }

    private static void mergeWithoutGrants(Document target, JsonObject source) {
        Document update = MongoJson.toDocument(source);
        update.remove("activeGrants");
        update.remove("_id");
        for (Map.Entry<String, Object> entry : update.entrySet()) {
            target.put(entry.getKey(), entry.getValue());
        }
    }

    // ── Alts: other profiles sharing a known IP ────────────────────────────────

    public List<JsonObject> findAlts(String uuid) {
        Document profile = findDocument(uuid);
        if (profile == null) return List.of();

        List<String> knownIps = profile.getList("knownIps", String.class, List.of());
        if (knownIps.isEmpty()) return List.of();

        List<JsonObject> alts = new ArrayList<>();
        for (Document doc : collection.find(Filters.and(Filters.ne("uuid", uuid), Filters.in("knownIps", knownIps)))) {
            alts.add(MongoJson.toJson(doc));
        }
        return alts;
    }

    // ── Grants ─────────────────────────────────────────────────────────────────

    public JsonArray getGrants(String uuid) {
        JsonArray array = new JsonArray();
        Document profile = findDocument(uuid);
        if (profile == null) return array;
        for (Document grant : grantsOf(profile)) array.add(MongoJson.toJson(new Document(grant)));
        return array;
    }

    public boolean addGrant(String uuid, JsonObject grant) {
        return collection.updateOne(byUuid(uuid), Updates.push("activeGrants", MongoJson.toDocument(grant)))
                .getMatchedCount() > 0;
    }

    /** Applies {@code patch}'s fields to the grant with {@code grantId}. */
    public boolean updateGrant(String uuid, String grantId, JsonObject patch) {
        Document profile = findDocument(uuid);
        if (profile == null) return false;

        List<Document> grants = grantsOf(profile);
        Document target = grants.stream().filter(g -> grantId.equals(g.getString("id"))).findFirst().orElse(null);
        if (target == null) return false;

        for (Map.Entry<String, JsonElement> entry : patch.entrySet()) {
            target.put(entry.getKey(), toBsonValue(entry.getValue()));
        }
        collection.updateOne(byUuid(uuid), Updates.set("activeGrants", grants));
        return true;
    }

    /** Marks every active grant removed.  Returns how many were removed. */
    public int clearGrants(String uuid, String removedBy, long removedAt, String removedReason) {
        Document profile = findDocument(uuid);
        if (profile == null) return 0;

        List<Document> grants = grantsOf(profile);
        long now = System.currentTimeMillis();
        int count = 0;
        for (Document grant : grants) {
            if (Boolean.TRUE.equals(grant.getBoolean("removed"))) continue;
            long end = grant.get("end") instanceof Number number ? number.longValue() : -1L;
            if (end != -1 && end < now) continue;

            grant.put("removed", true);
            grant.put("removedBy", removedBy);
            grant.put("removedAt", removedAt);
            grant.put("removedReason", removedReason);
            count++;
        }

        if (count > 0) collection.updateOne(byUuid(uuid), Updates.set("activeGrants", grants));
        return count;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Document findDocument(String uuid) {
        return collection.find(byUuid(uuid)).first();
    }

    private static List<Document> grantsOf(Document profile) {
        List<Document> grants = profile.getList("activeGrants", Document.class);
        return grants != null ? new ArrayList<>(grants) : new ArrayList<>();
    }

    /**
     * Keeps JSON numbers numeric and booleans boolean.  The Spring version stored every patched
     * value except booleans and removedAt as a string.
     */
    private static Object toBsonValue(JsonElement value) {
        if (value == null || value.isJsonNull()) return null;
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) {
                double number = primitive.getAsDouble();
                if (number == Math.rint(number) && !Double.isInfinite(number)) return primitive.getAsLong();
                return number;
            }
            return primitive.getAsString();
        }
        return Document.parse("{\"v\":" + value + "}").get("v");
    }

    private static Bson byUuid(String uuid) {
        return Filters.eq("uuid", uuid);
    }
}
