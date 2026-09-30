package games.sparking.altara.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.presence.PresenceService;
import games.sparking.altara.redis.RedisService;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.report.Report;
import games.sparking.altara.report.ReportCategory;
import games.sparking.altara.report.packet.ReportEngagePacket;
import games.sparking.altara.report.packet.ReportUpdatedPacket;
import games.sparking.altara.repository.ReportRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reports.  Creation merges onto an existing pending report against the same suspect in
 * the same group (so five people reporting one chat message becomes one report with five
 * reasons, not five). Claiming is optimistic — {@code ReportRepository#claim} is the only
 * place two servers can race, and losing that race just means trying the next candidate.
 */
@Singleton
@RequiredArgsConstructor
@Slf4j
public class ReportWebService {

    /** An IN_PROGRESS report quiet this long, whose handler is offline, is abandoned and released. */
    private static final long STALE_IN_PROGRESS_MS = 5 * 60 * 1000L;
    private static final int CLAIM_ATTEMPTS = 6;

    private final ReportRepository reportRepository;
    private final RedisService redisService;

    // ── Create ─────────────────────────────────────────────────────────────────

    public Optional<JsonObject> create(JsonObject request) {
        String suspectUuid = request.get("suspectUuid").getAsString();
        ReportCategory category = ReportCategory.valueOf(request.get("category").getAsString());
        String group = category.getGroup().name();

        JsonObject reason = new JsonObject();
        reason.addProperty("reporterUuid", string(request, "reporterUuid"));
        reason.addProperty("server", string(request, "reporterServer"));
        reason.addProperty("category", category.name());
        reason.addProperty("reportedAt", System.currentTimeMillis());

        List<JsonObject> incomingMessages = new ArrayList<>();
        if (request.has("messages") && request.get("messages").isJsonArray()) {
            for (JsonElement element : request.get("messages").getAsJsonArray()) {
                incomingMessages.add(element.getAsJsonObject());
            }
        }

        Optional<JsonObject> target = reportRepository.findMergeTarget(suspectUuid, group);
        JsonObject saved;
        boolean isNew;

        if (target.isPresent()) {
            saved = mergeOnto(target.get(), reason, incomingMessages, string(request, "reporterUuid"));
            isNew = false;
        } else {
            JsonObject fresh = new JsonObject();
            fresh.addProperty("id", UUID.randomUUID().toString());
            fresh.addProperty("suspectUuid", suspectUuid);
            fresh.addProperty("group", group);

            JsonArray reasons = new JsonArray();
            reasons.add(reason);
            fresh.add("reasons", reasons);

            JsonArray messages = new JsonArray();
            for (JsonObject message : incomingMessages) {
                if (string(request, "reporterUuid") != null) {
                    addReportedBy(message, string(request, "reporterUuid"));
                }
                messages.add(message);
            }
            fresh.add("messages", messages);

            fresh.addProperty("status", "PENDING");
            fresh.addProperty("statusTime", System.currentTimeMillis());
            fresh.addProperty("createdAt", System.currentTimeMillis());
            saved = reportRepository.insert(fresh);
            isNew = true;
        }

        publish(new ReportUpdatedPacket(saved.get("id").getAsString(), "PENDING", null, null, isNew, null));
        return Optional.of(saved);
    }

    /** Folds a new reason and any genuinely-new chat messages onto an existing pending report. */
    private JsonObject mergeOnto(JsonObject existing, JsonObject reason, List<JsonObject> incomingMessages, String reporterUuid) {
        JsonArray reasons = existing.has("reasons") && existing.get("reasons").isJsonArray()
                ? existing.get("reasons").getAsJsonArray() : new JsonArray();
        reasons.add(reason);

        JsonArray messages = existing.has("messages") && existing.get("messages").isJsonArray()
                ? existing.get("messages").getAsJsonArray() : new JsonArray();

        for (JsonObject incoming : incomingMessages) {
            JsonObject match = findSameMessage(messages, incoming);
            if (match != null) {
                if (reporterUuid != null) addReportedBy(match, reporterUuid);
            } else {
                if (reporterUuid != null) addReportedBy(incoming, reporterUuid);
                messages.add(incoming);
            }
        }

        return reportRepository.mergeOnto(existing.get("id").getAsString(), reasons, messages)
                .orElse(existing);
    }

    private static JsonObject findSameMessage(JsonArray messages, JsonObject candidate) {
        String sender = string(candidate, "senderUuid");
        long sentAt = candidate.has("sentAt") ? candidate.get("sentAt").getAsLong() : -1;
        String text = string(candidate, "message");

        for (JsonElement element : messages) {
            JsonObject existing = element.getAsJsonObject();
            boolean sameSender = java.util.Objects.equals(sender, string(existing, "senderUuid"));
            boolean sameTime = existing.has("sentAt") && existing.get("sentAt").getAsLong() == sentAt;
            boolean sameText = java.util.Objects.equals(text, string(existing, "message"));
            if (sameSender && sameTime && sameText) return existing;
        }
        return null;
    }

    private static void addReportedBy(JsonObject message, String reporterUuid) {
        JsonArray reportedBy = message.has("reportedBy") && message.get("reportedBy").isJsonArray()
                ? message.get("reportedBy").getAsJsonArray() : new JsonArray();
        for (JsonElement element : reportedBy) {
            if (reporterUuid.equals(element.getAsString())) return;
        }
        reportedBy.add(reporterUuid);
        message.add("reportedBy", reportedBy);
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    public Optional<JsonObject> getById(String id) {
        return reportRepository.findById(id);
    }

    public JsonArray listUnhandled() {
        return reportRepository.findUnhandled();
    }

    public Optional<JsonObject> getByHandler(String staffUuid) {
        return reportRepository.findByHandler(staffUuid);
    }

    public Optional<JsonObject> getActiveForSuspect(String suspectUuid) {
        return reportRepository.findActiveBySuspect(suspectUuid);
    }

    /**
     * The live queue for the staff website: runs the same stale/expiry sweep the claim path
     * does, then returns pending reports highest-priority first and in-progress reports
     * oldest-claim first, each annotated with its computed {@code priority} and live
     * presence ({@code suspectOnline}, {@code suspectServer}, {@code handlerOnline}).
     */
    public JsonObject queue() {
        List<JsonObject> pending = sweep();
        pending.sort(Comparator.comparingDouble((JsonObject r) -> Report.fromJson(r).getPriority()).reversed());

        List<JsonObject> inProgress = new ArrayList<>();
        for (JsonElement element : reportRepository.findUnhandled()) {
            JsonObject raw = element.getAsJsonObject();
            if ("IN_PROGRESS".equals(string(raw, "status"))) inProgress.add(raw);
        }
        inProgress.sort(Comparator.comparingLong((JsonObject r) -> Report.fromJson(r).getStatusTime()));

        JsonArray pendingArray = new JsonArray();
        pending.forEach(r -> pendingArray.add(annotate(r)));
        JsonArray inProgressArray = new JsonArray();
        inProgress.forEach(r -> inProgressArray.add(annotate(r)));

        JsonObject result = new JsonObject();
        result.add("pending", pendingArray);
        result.add("inProgress", inProgressArray);
        return result;
    }

    public JsonArray search(List<String> statuses, String group, String suspectUuid, String handlerUuid,
                            Long before, int limit) {
        JsonArray raw = reportRepository.search(statuses, group, suspectUuid, handlerUuid, before, limit);
        JsonArray annotated = new JsonArray();
        for (JsonElement element : raw) {
            JsonObject report = element.getAsJsonObject();
            report.addProperty("priority", Report.fromJson(report).getPriority());
            annotated.add(report);
        }
        return annotated;
    }

    private JsonObject annotate(JsonObject raw) {
        JsonObject report = raw.deepCopy();
        report.addProperty("priority", Report.fromJson(raw).getPriority());

        String suspect = string(raw, "suspectUuid");
        String suspectServer = suspect != null ? presenceServer(suspect) : null;
        report.addProperty("suspectOnline", suspectServer != null);
        report.addProperty("suspectServer", suspectServer);

        String handler = string(raw, "handler");
        report.addProperty("handlerOnline", handler != null && presenceServer(handler) != null);
        return report;
    }

    // ── Claim (no in-game action) ──────────────────────────────────────────────

    /**
     * Claims the highest-priority open report, releasing or expiring stale/abandoned ones
     * it encounters along the way.  Retries a handful of times if another server wins the
     * atomic claim on the same candidate first.
     */
    public Optional<JsonObject> claimNext(String staffUuid, String server, String source) {
        for (int attempt = 0; attempt < CLAIM_ATTEMPTS; attempt++) {
            JsonObject candidate = nextCandidate();
            if (candidate == null) return Optional.empty();

            String id = candidate.get("id").getAsString();
            if (reportRepository.claim(id, staffUuid, server)) {
                return finishClaim(id, staffUuid, server, source);
            }
            // Someone else claimed it between selection and claim — loop and pick the next one.
        }
        return Optional.empty();
    }

    /** Claims one specific report without pulling anyone in-game (e.g. a chat report dealt with entirely on the website). */
    public Optional<JsonObject> claimSpecific(String id, String staffUuid, String server, String source) {
        if (!reportRepository.claim(id, staffUuid, server)) return Optional.empty();
        return finishClaim(id, staffUuid, server, source);
    }

    private Optional<JsonObject> finishClaim(String id, String staffUuid, String server, String source) {
        Optional<JsonObject> claimed = reportRepository.findById(id);
        claimed.ifPresent(report -> publish(new ReportUpdatedPacket(id, "IN_PROGRESS", staffUuid, server, false, source)));
        return claimed;
    }

    /** Keeps a claim alive while someone works it on the website (the stale sweep skips it). */
    public boolean heartbeat(String id, String staffUuid) {
        return reportRepository.refreshClaim(id, staffUuid, null);
    }

    // ── Engage (claim + pull the staff member in-game) ─────────────────────────

    public enum EngageOutcome {
        /** Claimed (or already theirs) and the engage packet went out. */
        ENGAGED,
        NOT_FOUND,
        /** Already resolved/expired. */
        CLOSED,
        /** The staff member isn't connected to any server. */
        STAFF_OFFLINE,
        /** Someone else is handling it. */
        TAKEN,
        /** The staff member is already handling a different report ({@code report} is that one). */
        BUSY,
        /** Nothing in the queue. */
        EMPTY
    }

    public record EngageResult(EngageOutcome outcome, JsonObject report) {}

    /**
     * Hands one specific report to a staff member and pulls them into it in-game: staff
     * mode, the report panel, and a follow to wherever the suspect is — for every report
     * group, chat included.  Re-engaging a report the staff member already holds (say they
     * claimed it on the website and now want to go look) is allowed and just refreshes it.
     *
     * @param force release whatever other report the staff member is holding first,
     *              instead of answering {@link EngageOutcome#BUSY}
     */
    public EngageResult engage(String id, String staffUuid, boolean force) {
        String staffServer = presenceServer(staffUuid);
        if (staffServer == null) return new EngageResult(EngageOutcome.STAFF_OFFLINE, null);

        Optional<JsonObject> found = reportRepository.findById(id);
        if (found.isEmpty()) return new EngageResult(EngageOutcome.NOT_FOUND, null);

        Report report = Report.fromJson(found.get());
        if (!report.isActive()) return new EngageResult(EngageOutcome.CLOSED, found.get());

        EngageResult busy = releaseOtherClaim(staffUuid, id, force);
        if (busy != null) return busy;

        boolean ours;
        if (report.isClaimed()) {
            ours = staffUuid.equals(report.getHandler()) && reportRepository.refreshClaim(id, staffUuid, staffServer);
        } else {
            ours = reportRepository.claim(id, staffUuid, staffServer);
        }
        if (!ours) return new EngageResult(EngageOutcome.TAKEN, reportRepository.findById(id).orElse(null));

        return finishEngage(id, staffUuid, staffServer);
    }

    /** Claims the highest-priority report for a staff member and pulls them into it in-game. */
    public EngageResult engageNext(String staffUuid, boolean force) {
        String staffServer = presenceServer(staffUuid);
        if (staffServer == null) return new EngageResult(EngageOutcome.STAFF_OFFLINE, null);

        EngageResult busy = releaseOtherClaim(staffUuid, null, force);
        if (busy != null) return busy;

        for (int attempt = 0; attempt < CLAIM_ATTEMPTS; attempt++) {
            JsonObject candidate = nextCandidate();
            if (candidate == null) return new EngageResult(EngageOutcome.EMPTY, null);

            String id = candidate.get("id").getAsString();
            if (reportRepository.claim(id, staffUuid, staffServer)) {
                return finishEngage(id, staffUuid, staffServer);
            }
        }
        return new EngageResult(EngageOutcome.EMPTY, null);
    }

    /**
     * If the staff member already holds a report other than {@code keepId}: returns a
     * {@code BUSY} result, or — when {@code force} — releases it back to the queue and
     * returns {@code null} so the caller carries on.
     */
    private EngageResult releaseOtherClaim(String staffUuid, String keepId, boolean force) {
        Optional<JsonObject> current = reportRepository.findByHandler(staffUuid);
        if (current.isEmpty()) return null;

        String currentId = string(current.get(), "id");
        if (currentId == null || currentId.equals(keepId)) return null;

        if (!force) return new EngageResult(EngageOutcome.BUSY, current.get());

        reportRepository.release(currentId).ifPresent(released ->
                publish(new ReportUpdatedPacket(currentId, "PENDING", null, null, false, null)));
        return null;
    }

    private EngageResult finishEngage(String id, String staffUuid, String staffServer) {
        Optional<JsonObject> claimed = reportRepository.findById(id);
        if (claimed.isEmpty()) return new EngageResult(EngageOutcome.NOT_FOUND, null);

        publish(new ReportUpdatedPacket(id, "IN_PROGRESS", staffUuid, staffServer, false, null));
        publish(new ReportEngagePacket(id, staffUuid));
        return new EngageResult(EngageOutcome.ENGAGED, claimed.get());
    }

    // ── Queue sweep ────────────────────────────────────────────────────────────

    /** The best PENDING candidate after a {@link #sweep()}, or {@code null} if the queue is empty. */
    private JsonObject nextCandidate() {
        return sweep().stream()
                .max(Comparator.comparingDouble(candidate -> Report.fromJson(candidate).getPriority()))
                .orElse(null);
    }

    /**
     * Scans every open report, releasing abandoned IN_PROGRESS ones and expiring anything
     * whose priority has decayed below the floor, and returns what's left PENDING.
     *
     * <p>A claim counts as abandoned only when it has had no activity (claim, engage or
     * website heartbeat) for {@link #STALE_IN_PROGRESS_MS} <em>and</em> its handler isn't
     * connected to any server — an in-game handler who is still online keeps their report
     * no matter how long the review takes, and one hopping servers to follow a suspect
     * never loses it.
     */
    private List<JsonObject> sweep() {
        List<JsonObject> candidates = new ArrayList<>();

        for (JsonElement element : reportRepository.findUnhandled()) {
            JsonObject raw = element.getAsJsonObject();
            Report report = Report.fromJson(raw);
            String id = report.getId();

            if (report.isClaimed()) {
                boolean quiet = System.currentTimeMillis() - report.getStatusTime() > STALE_IN_PROGRESS_MS;
                if (!quiet || presenceServer(report.getHandler()) != null) continue; // still being worked

                if (report.isExpired()) {
                    reportRepository.expire(id);
                } else {
                    reportRepository.release(id).ifPresent(released ->
                            publish(new ReportUpdatedPacket(id, "PENDING", null, null, false, null)));
                }
                continue;
            }

            if (report.isExpired()) {
                reportRepository.expire(id);
                continue;
            }

            candidates.add(raw);
        }

        return candidates;
    }

    // ── Resolve / release ────────────────────────────────────────────────────────

    public Optional<JsonObject> resolve(String id, String staffUuid, String status, String reasonDetail, String source) {
        Optional<JsonObject> result = reportRepository.resolve(id, status, staffUuid, reasonDetail);
        result.ifPresent(report -> publish(new ReportUpdatedPacket(id, status, staffUuid, null, false, source)));
        return result;
    }

    public Optional<JsonObject> release(String id, String source) {
        Optional<JsonObject> result = reportRepository.release(id);
        result.ifPresent(report -> publish(new ReportUpdatedPacket(id, "PENDING", null, null, false, source)));
        return result;
    }

    /** The server a player is on right now, or {@code null} — never throws on a bad UUID or Redis hiccup. */
    private static String presenceServer(String uuid) {
        try {
            return PresenceService.getServer(UUID.fromString(uuid));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private void publish(Packet packet) {
        try {
            redisService.publish(packet);
        } catch (Exception e) {
            log.warn("Could not publish {}: {}", packet.getClass().getSimpleName(), e.getMessage());
        }
    }
}
