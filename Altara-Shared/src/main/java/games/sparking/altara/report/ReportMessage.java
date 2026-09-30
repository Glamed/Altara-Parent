package games.sparking.altara.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A single cached chat message attached to a {@link ReportGroup#CHAT} report, so staff
 * (or the website) can see the surrounding conversation rather than just the one line
 * that triggered the report.
 */
@Getter
@NoArgsConstructor
public class ReportMessage {

    private String senderUuid;
    private long sentAt;
    private String message;
    private List<String> recipients = new ArrayList<>();
    private List<String> reportedBy = new ArrayList<>();

    /**
     * Reserved for a future automated risk score (e.g. a machine-learning pass over chat).
     * Always {@code 0} today — nothing currently writes a non-zero value here.
     */
    private int riskLevel = 0;

    public ReportMessage(String senderUuid, long sentAt, String message, List<String> recipients) {
        this.senderUuid = senderUuid;
        this.sentAt = sentAt;
        this.message = message;
        this.recipients = new ArrayList<>(recipients);
    }

    /** Same sender + timestamp + text is treated as the same cached message. */
    public boolean sameMessageAs(ReportMessage other) {
        return other != null
                && sentAt == other.sentAt
                && java.util.Objects.equals(senderUuid, other.senderUuid)
                && java.util.Objects.equals(message, other.message);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("senderUuid", senderUuid);
        obj.addProperty("sentAt", sentAt);
        obj.addProperty("message", message);
        obj.addProperty("riskLevel", riskLevel);

        JsonArray recipientsArray = new JsonArray();
        recipients.forEach(recipientsArray::add);
        obj.add("recipients", recipientsArray);

        JsonArray reportedByArray = new JsonArray();
        reportedBy.forEach(reportedByArray::add);
        obj.add("reportedBy", reportedByArray);
        return obj;
    }

    public static ReportMessage fromJson(JsonObject obj) {
        ReportMessage message = new ReportMessage();
        message.senderUuid = obj.has("senderUuid") && !obj.get("senderUuid").isJsonNull()
                ? obj.get("senderUuid").getAsString() : null;
        message.sentAt = obj.has("sentAt") ? obj.get("sentAt").getAsLong() : 0L;
        message.message = obj.has("message") && !obj.get("message").isJsonNull()
                ? obj.get("message").getAsString() : "";
        message.riskLevel = obj.has("riskLevel") ? obj.get("riskLevel").getAsInt() : 0;

        List<String> recipients = new ArrayList<>();
        if (obj.has("recipients") && obj.get("recipients").isJsonArray()) {
            obj.get("recipients").getAsJsonArray().forEach(e -> recipients.add(e.getAsString()));
        }
        message.recipients = recipients;

        List<String> reportedBy = new ArrayList<>();
        if (obj.has("reportedBy") && obj.get("reportedBy").isJsonArray()) {
            obj.get("reportedBy").getAsJsonArray().forEach(e -> reportedBy.add(e.getAsString()));
        }
        message.reportedBy = reportedBy;

        return message;
    }
}
