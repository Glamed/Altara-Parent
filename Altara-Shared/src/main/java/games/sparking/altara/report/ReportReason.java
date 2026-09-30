package games.sparking.altara.report;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One reporter's contribution to a {@link Report}.  Several reasons can stack onto the
 * same report when multiple players report the same suspect for the same category
 * before staff pick it up — see {@code ReportWebService} for the merge logic.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportReason {

    /** UUID string of the reporter, or {@code null} for an automated/console report. */
    private String reporterUuid;

    /** Server the reporter was on when they filed it. */
    private String server;

    /** {@link ReportCategory} enum name. */
    private String category;

    private long reportedAt;

    public long getElapsedMillis() {
        return Math.max(0L, System.currentTimeMillis() - reportedAt);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("reporterUuid", reporterUuid);
        obj.addProperty("server", server);
        obj.addProperty("category", category);
        obj.addProperty("reportedAt", reportedAt);
        return obj;
    }

    public static ReportReason fromJson(JsonObject obj) {
        String reporterUuid = obj.has("reporterUuid") && !obj.get("reporterUuid").isJsonNull()
                ? obj.get("reporterUuid").getAsString() : null;
        String server = obj.has("server") && !obj.get("server").isJsonNull()
                ? obj.get("server").getAsString() : null;
        String category = obj.get("category").getAsString();
        long reportedAt = obj.has("reportedAt") ? obj.get("reportedAt").getAsLong() : System.currentTimeMillis();
        return new ReportReason(reporterUuid, server, category, reportedAt);
    }
}
