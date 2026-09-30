package games.sparking.altara.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@code punishments} collection.  {@code _id} mirrors the punishment's own {@code id}.
 *
 * <pre>
 * {
 *   "id": "...", "playerUuid": "...", "staffUuid": "...", "infractionType": "PROFANITY",
 *   "actions": [ { "type": "CHAT_RESTRICTION", "duration": 1800000 } ],
 *   "message": null, "notes": null, "issuedAt": 1234567890000,
 *   "removed": false, "removedAt": -1, "removedBy": null, "notified": false
 * }
 * </pre>
 * Action durations are relative to {@code issuedAt}; {@code -1} is permanent.
 */
@Singleton
public class PunishmentRepository {

    private final MongoCollection<Document> collection;

    public PunishmentRepository(MongoDatabase database) {
        this.collection = database.getCollection("punishments");
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public Optional<JsonObject> findById(String id) {
        return Optional.ofNullable(collection.find(byId(id)).first()).map(MongoJson::toJson);
    }

    public JsonObject insert(JsonObject punishment) {
        String id = punishment.get("id").getAsString();
        Document doc = MongoJson.toDocument(punishment);
        doc.put("_id", id);
        collection.insertOne(doc);
        return findById(id).orElse(punishment);
    }

    /** Soft-deletes (revokes) a punishment. */
    public Optional<JsonObject> revoke(String id, String removedBy) {
        return updateAndFind(id, Updates.combine(
                Updates.set("removed", true),
                Updates.set("removedAt", System.currentTimeMillis()),
                Updates.set("removedBy", removedBy)));
    }

    /** Records that the player has been shown this punishment. */
    public Optional<JsonObject> markNotified(String id) {
        return updateAndFind(id, Updates.set("notified", true));
    }

    /** Partial update.  Only {@code infractionType}, {@code message}, {@code notes} and {@code actions} are written. */
    public Optional<JsonObject> patch(String id, JsonObject updates) {
        List<Bson> changes = new ArrayList<>();
        if (updates.has("infractionType") && !updates.get("infractionType").isJsonNull()) {
            changes.add(Updates.set("infractionType", updates.get("infractionType").getAsString()));
        }
        if (updates.has("message")) changes.add(Updates.set("message", MongoJson.string(updates, "message")));
        if (updates.has("notes")) changes.add(Updates.set("notes", MongoJson.string(updates, "notes")));
        if (updates.has("actions") && updates.get("actions").isJsonArray()) {
            changes.add(Updates.set("actions", Document.parse("{\"v\":" + updates.get("actions") + "}").get("v")));
        }

        if (changes.isEmpty()) return findById(id);
        return updateAndFind(id, Updates.combine(changes));
    }

    private Optional<JsonObject> updateAndFind(String id, Bson update) {
        if (collection.updateOne(byId(id), update).getMatchedCount() == 0) return Optional.empty();
        return findById(id);
    }

    // ── Leaderboards ──────────────────────────────────────────────────────────

    /**
     * Every non-revoked punishment issued in the last {@code sinceMillis} ms ({@code 0} or
     * less for all time).  Leaderboard counts are then built in Java from these raw records
     * (see {@code PunishmentWebService}) — the collection is small enough that this is
     * simpler and safer than a hand-written aggregation pipeline, and keeps the counting
     * logic in one place instead of duplicated across a Mongo pipeline and Java fallback.
     */
    public JsonArray findIssuedSince(long sinceMillis) {
        Bson filter = sinceMillis > 0
                ? Filters.and(Filters.eq("removed", false),
                        Filters.gte("issuedAt", System.currentTimeMillis() - sinceMillis))
                : Filters.eq("removed", false);
        return MongoJson.toArray(collection.find(filter));
    }

    // ── Player queries ─────────────────────────────────────────────────────────

    /** Every punishment record for a player. */
    public JsonArray findByPlayer(String playerUuid) {
        return MongoJson.toArray(collection.find(Filters.eq("playerUuid", playerUuid)));
    }

    /** Punishments with at least one restriction still running. */
    public JsonArray findActiveByPlayer(String playerUuid) {
        JsonArray result = new JsonArray();
        for (Document doc : collection.find(Filters.and(Filters.eq("playerUuid", playerUuid), Filters.eq("removed", false)))) {
            JsonObject punishment = MongoJson.toJson(doc);
            if (hasRunningAction(punishment, null)) result.add(punishment);
        }
        return result;
    }

    /** Whether the player has a suspension that is still running. */
    public boolean isBanned(String playerUuid) {
        for (JsonElement element : findActiveByPlayer(playerUuid)) {
            if (hasRunningAction(element.getAsJsonObject(), "SUSPENSION")) return true;
        }
        return false;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static Bson byId(String id) {
        return Filters.eq("id", id);
    }

    /**
     * Whether any action (of {@code type}, or of any type when {@code null}) is still running.
     * Mirrors {@code Punishment#isActive()} / {@code RestrictionAction#hasExpired}.
     */
    private static boolean hasRunningAction(JsonObject punishment, String type) {
        if (punishment.has("removed") && punishment.get("removed").getAsBoolean()) return false;
        if (!punishment.has("actions") || !punishment.get("actions").isJsonArray()) return false;

        long issuedAt = punishment.has("issuedAt") ? punishment.get("issuedAt").getAsLong() : 0L;
        long now = System.currentTimeMillis();
        for (JsonElement element : punishment.get("actions").getAsJsonArray()) {
            JsonObject action = element.getAsJsonObject();
            if (type != null && !type.equals(MongoJson.string(action, "type"))) continue;

            long duration = action.has("duration") ? action.get("duration").getAsLong() : 0L;
            if (duration == -1L) return true;
            if (duration > 0L && now <= issuedAt + duration) return true;
        }
        return false;
    }
}
