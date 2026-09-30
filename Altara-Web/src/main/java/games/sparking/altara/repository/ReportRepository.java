package games.sparking.altara.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@code reports} collection.  {@code _id} mirrors the report's own {@code id}.
 *
 * <pre>
 * {
 *   "id": "...", "suspectUuid": "...", "group": "CHAT"|"GAMEPLAY",
 *   "reasons": [ { "reporterUuid", "server", "category", "reportedAt" } ],
 *   "messages": [ { "senderUuid", "sentAt", "message", "recipients": [...], "reportedBy": [...], "riskLevel" } ],
 *   "status": "PENDING", "handler": null, "handlerServer": null,
 *   "statusReason": null, "statusTime": 0, "createdAt": 0
 * }
 * </pre>
 */
@Singleton
public class ReportRepository {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "IN_PROGRESS");

    private final MongoCollection<Document> collection;

    public ReportRepository(MongoDatabase database) {
        this.collection = database.getCollection("reports");
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    public Optional<JsonObject> findById(String id) {
        return Optional.ofNullable(collection.find(byId(id)).first()).map(MongoJson::toJson);
    }

    public JsonObject insert(JsonObject report) {
        String id = report.get("id").getAsString();
        Document doc = MongoJson.toDocument(report);
        doc.put("_id", id);
        collection.insertOne(doc);
        return findById(id).orElse(report);
    }

    /** A still-open (not yet claimed) report against this suspect, in the same group, to merge onto. */
    public Optional<JsonObject> findMergeTarget(String suspectUuid, String group) {
        return Optional.ofNullable(collection.find(Filters.and(
                Filters.eq("suspectUuid", suspectUuid),
                Filters.eq("group", group),
                Filters.eq("status", "PENDING")
        )).first()).map(MongoJson::toJson);
    }

    /**
     * Replaces the {@code reasons} and {@code messages} arrays with the already-merged
     * versions (deduplication happens in {@code ReportWebService}, in Java, since Mongo
     * can't easily de-duplicate nested array objects) and forces the status back to PENDING.
     */
    public Optional<JsonObject> mergeOnto(String id, JsonArray reasons, JsonArray messages) {
        return updateAndFind(id, Updates.combine(
                Updates.set("reasons", arrayValue(reasons)),
                Updates.set("messages", arrayValue(messages)),
                Updates.set("status", "PENDING")
        ));
    }

    /** Parses a Gson array into a BSON-compatible value {@code Updates.set} can store. */
    private static Object arrayValue(JsonArray array) {
        return Document.parse("{\"v\":" + array + "}").get("v");
    }

    // ── Claim / release / resolve ───────────────────────────────────────────────

    /** Atomically claims an unclaimed PENDING report. Returns {@code false} if another server won the race. */
    public boolean claim(String id, String staffUuid, String server) {
        Bson filter = Filters.and(
                byId(id),
                Filters.eq("status", "PENDING"),
                Filters.or(
                        Filters.eq("handler", null),
                        Filters.exists("handler", false)
                )
        );
        Bson update = Updates.combine(
                Updates.set("status", "IN_PROGRESS"),
                Updates.set("handler", staffUuid),
                Updates.set("handlerServer", server),
                Updates.set("statusTime", System.currentTimeMillis())
        );
        return collection.updateOne(filter, update).getModifiedCount() > 0;
    }

    /**
     * Keeps an existing claim alive for the staff member who already holds it: bumps
     * {@code statusTime} (so the stale sweep leaves it alone) and, when {@code server} is
     * non-null, records the server they're on now.  Returns {@code false} if the report
     * isn't IN_PROGRESS under this handler any more.
     */
    public boolean refreshClaim(String id, String staffUuid, String server) {
        Bson filter = Filters.and(
                byId(id),
                Filters.eq("status", "IN_PROGRESS"),
                Filters.eq("handler", staffUuid)
        );
        Bson update = server == null
                ? Updates.set("statusTime", System.currentTimeMillis())
                : Updates.combine(
                        Updates.set("handlerServer", server),
                        Updates.set("statusTime", System.currentTimeMillis()));
        return collection.updateOne(filter, update).getMatchedCount() > 0;
    }

    /** Puts the report back in the queue, clearing its handler. */
    public Optional<JsonObject> release(String id) {
        return updateAndFind(id, Updates.combine(
                Updates.set("status", "PENDING"),
                Updates.set("handler", null),
                Updates.set("handlerServer", null),
                Updates.set("statusTime", System.currentTimeMillis())
        ));
    }

    /** Forces a report to EXPIRED regardless of its current handler state — used for stale cleanup. */
    public Optional<JsonObject> expire(String id) {
        return updateAndFind(id, Updates.combine(
                Updates.set("status", "EXPIRED"),
                Updates.set("statusTime", System.currentTimeMillis())
        ));
    }

    public Optional<JsonObject> resolve(String id, String status, String staffUuid, String reasonDetail) {
        return updateAndFind(id, Updates.combine(
                Updates.set("status", status),
                Updates.set("handler", staffUuid),
                Updates.set("statusReason", reasonDetail),
                Updates.set("statusTime", System.currentTimeMillis())
        ));
    }

    private Optional<JsonObject> updateAndFind(String id, Bson update) {
        if (collection.updateOne(byId(id), update).getMatchedCount() == 0) return Optional.empty();
        return findById(id);
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /** Every report that is PENDING or IN_PROGRESS — the live queue, before priority ranking. */
    public JsonArray findUnhandled() {
        return MongoJson.toArray(collection.find(Filters.in("status", ACTIVE_STATUSES)));
    }

    public Optional<JsonObject> findByHandler(String staffUuid) {
        return Optional.ofNullable(collection.find(Filters.and(
                Filters.eq("status", "IN_PROGRESS"),
                Filters.eq("handler", staffUuid)
        )).first()).map(MongoJson::toJson);
    }

    /** The active (pending or in-progress) report against a suspect, any group — for the follow trigger. */
    public Optional<JsonObject> findActiveBySuspect(String suspectUuid) {
        // "IN_PROGRESS" sorts before "PENDING", so a report someone is handling wins over a
        // separate pending one against the same suspect (e.g. a gameplay report being worked
        // while a fresh chat report waits).
        return Optional.ofNullable(collection.find(Filters.and(
                Filters.eq("suspectUuid", suspectUuid),
                Filters.in("status", ACTIVE_STATUSES)
        )).sort(Sorts.ascending("status")).first()).map(MongoJson::toJson);
    }

    /**
     * History / search for the website.  Every filter is optional; results are newest
     * first.  {@code before} is a {@code createdAt} cursor for paging (pass the last
     * result's {@code createdAt} to get the next page).
     */
    public JsonArray search(List<String> statuses, String group, String suspectUuid, String handlerUuid,
                            Long before, int limit) {
        List<Bson> filters = new ArrayList<>();
        if (statuses != null && !statuses.isEmpty()) filters.add(Filters.in("status", statuses));
        if (group != null) filters.add(Filters.eq("group", group));
        if (suspectUuid != null) filters.add(Filters.eq("suspectUuid", suspectUuid));
        if (handlerUuid != null) filters.add(Filters.eq("handler", handlerUuid));
        if (before != null) filters.add(Filters.lt("createdAt", before));

        Bson filter = filters.isEmpty() ? new Document() : Filters.and(filters);
        return MongoJson.toArray(collection.find(filter).sort(Sorts.descending("createdAt")).limit(limit));
    }

    private static Bson byId(String id) {
        return Filters.eq("id", id);
    }
}
