package games.sparking.altara.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player report, possibly backed by several stacked {@link ReportReason}s from
 * different reporters.  Cross-network: the {@link #handlerServer} is what lets a
 * suspect's server change trigger the handling staff member to follow them (see
 * {@code ReportFollowService} in Altara-Paper).
 */
@Getter
@Setter
@NoArgsConstructor
public class Report {

    private String id;

    /** UUID string of the reported player. */
    private String suspectUuid;

    /** Whether chat history is relevant — decides which reasons/messages apply. */
    private ReportGroup group;

    private List<ReportReason> reasons = new ArrayList<>();

    /** Only populated for {@link ReportGroup#CHAT} reports. */
    private List<ReportMessage> messages = new ArrayList<>();

    private ReportStatus status = ReportStatus.PENDING;

    /** UUID string of the staff member currently handling this, or {@code null}. */
    private String handler;

    /** Server name the handler was on when they claimed it — the cross-network follow target. */
    private String handlerServer;

    /** {@link RejectionType} name, or an accepted infraction name, or {@code null}. */
    private String statusReason;

    private long statusTime;
    private long createdAt;

    public Report(UUID suspectUuid, ReportGroup group) {
        this.id = UUID.randomUUID().toString();
        this.suspectUuid = suspectUuid.toString();
        this.group = group;
        this.status = ReportStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.statusTime = this.createdAt;
    }

    public boolean isActive() {
        return status == ReportStatus.PENDING || status == ReportStatus.IN_PROGRESS;
    }

    public boolean isClaimed() {
        return status == ReportStatus.IN_PROGRESS && handler != null && !handler.isBlank();
    }

    /**
     * Ranks the queue: a base weight, plus each reason's {@link ReportCategory} priority
     * decayed by age (older, unclaimed reports still eventually surface, but fresh reports
     * from multiple people jump ahead), plus a bump per attached chat message so a longer
     * conversation trail counts for something. Higher is more urgent.
     */
    public double getPriority() {
        double priority = 30;

        for (ReportReason reason : reasons) {
            double ageDecay = Math.pow(0.95, reason.getElapsedMillis() / 60_000.0);
            ReportCategory category = parseCategory(reason.getCategory());
            int categoryPriority = category != null ? category.getPriority() : 1;
            priority += (5 + categoryPriority) * ageDecay;
        }

        priority += Math.min(messages.size(), 10) * 0.5;
        return priority;
    }

    /** Below this, an unclaimed report is stale enough to expire rather than keep queuing. */
    public boolean isExpired() {
        return getPriority() < 1;
    }

    private static ReportCategory parseCategory(String name) {
        try {
            return ReportCategory.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    // ── JSON ───────────────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("suspectUuid", suspectUuid);
        obj.addProperty("group", group != null ? group.name() : null);

        JsonArray reasonsArray = new JsonArray();
        for (ReportReason reason : reasons) reasonsArray.add(reason.toJson());
        obj.add("reasons", reasonsArray);

        JsonArray messagesArray = new JsonArray();
        for (ReportMessage message : messages) messagesArray.add(message.toJson());
        obj.add("messages", messagesArray);

        obj.addProperty("status", status != null ? status.name() : null);
        obj.addProperty("handler", handler);
        obj.addProperty("handlerServer", handlerServer);
        obj.addProperty("statusReason", statusReason);
        obj.addProperty("statusTime", statusTime);
        obj.addProperty("createdAt", createdAt);
        return obj;
    }

    public static Report fromJson(JsonObject obj) {
        Report report = new Report();
        report.id = str(obj, "id");
        report.suspectUuid = str(obj, "suspectUuid");
        report.group = obj.has("group") && !obj.get("group").isJsonNull()
                ? ReportGroup.valueOf(obj.get("group").getAsString()) : ReportGroup.GAMEPLAY;

        List<ReportReason> reasons = new ArrayList<>();
        if (obj.has("reasons") && obj.get("reasons").isJsonArray()) {
            for (JsonElement element : obj.get("reasons").getAsJsonArray()) {
                reasons.add(ReportReason.fromJson(element.getAsJsonObject()));
            }
        }
        report.reasons = reasons;

        List<ReportMessage> messages = new ArrayList<>();
        if (obj.has("messages") && obj.get("messages").isJsonArray()) {
            for (JsonElement element : obj.get("messages").getAsJsonArray()) {
                messages.add(ReportMessage.fromJson(element.getAsJsonObject()));
            }
        }
        report.messages = messages;

        report.status = ReportStatus.parse(str(obj, "status"), ReportStatus.PENDING);
        report.handler = str(obj, "handler");
        report.handlerServer = str(obj, "handlerServer");
        report.statusReason = str(obj, "statusReason");
        report.statusTime = obj.has("statusTime") ? obj.get("statusTime").getAsLong() : 0L;
        report.createdAt = obj.has("createdAt") ? obj.get("createdAt").getAsLong() : 0L;
        return report;
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
