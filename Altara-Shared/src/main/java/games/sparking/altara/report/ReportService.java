package games.sparking.altara.report;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Shared report service used by every Altara module.  Reports always live in MongoDB
 * behind the Web API — this class only talks to that API and keeps a small local cache
 * of "which staff member on this JVM is handling which report" so repeated commands
 * (e.g. {@code /reportclose}) don't need a network round trip.
 *
 * <p>The actual cross-server "handler follows the suspect" mechanic lives in
 * {@code games.sparking.altara.report.ReportFollowService} (Altara-Paper only), since it
 * needs Bukkit teleports and BungeeCord messaging this module can't depend on.
 */
public class ReportService {

    private static final String STAFF_PERMISSION = "altara.staff";

    /** Tags API calls made from in-game so the resulting packets don't echo back to the player. */
    public static final String SOURCE_GAME = "game";

    /** staffUuid -> the report they're currently handling on this JVM. */
    private final Map<UUID, Report> handling = new ConcurrentHashMap<>();

    /**
     * What to do when the website hands a report to a staff member on this server (put
     * them in staff mode, show the report, follow the suspect).  Set by Altara-Paper;
     * called off the main thread with a report already confirmed to be theirs.
     */
    @Setter
    private BiConsumer<UUID, Report> engageHandler;

    // ── Create ─────────────────────────────────────────────────────────────────

    /**
     * Submits a report.  {@code reporterUuid} is {@code null} for an automated/console
     * report.  The Web API merges this into an existing pending report against the same
     * suspect in the same group when one exists.
     */
    public Optional<Report> create(UUID reporterUuid, String reporterServer, UUID suspectUuid,
                                    ReportCategory category, List<ReportMessage> chatMessages) {
        JsonObject body = new JsonObject();
        body.addProperty("reporterUuid", reporterUuid != null ? reporterUuid.toString() : null);
        body.addProperty("reporterServer", reporterServer);
        body.addProperty("suspectUuid", suspectUuid.toString());
        body.addProperty("category", category.name());

        JsonArray messagesArray = new JsonArray();
        if (chatMessages != null) {
            for (ReportMessage message : chatMessages) messagesArray.add(message.toJson());
        }
        body.add("messages", messagesArray);

        RequestResponse response = RequestHandler.post("api/report", body);
        return response.wasSuccessful() ? Optional.of(Report.fromJson(response.asObject())) : Optional.empty();
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    public Optional<Report> getById(String id) {
        RequestResponse response = RequestHandler.get("api/report/%s", id);
        return response.wasSuccessful() ? Optional.of(Report.fromJson(response.asObject())) : Optional.empty();
    }

    /** The report this staff member is handling right now, checking the local cache first. */
    public Optional<Report> getHandling(UUID staffUuid) {
        Report cached = handling.get(staffUuid);
        if (cached != null) return Optional.of(cached);

        RequestResponse response = RequestHandler.get("api/report/handler/%s", staffUuid.toString());
        if (!response.wasSuccessful() || response.asObject() == null) return Optional.empty();

        Report report = Report.fromJson(response.asObject());
        handling.put(staffUuid, report);
        return Optional.of(report);
    }

    /** An active (pending or in-progress) report against this suspect, if any. */
    public Optional<Report> getActiveForSuspect(UUID suspectUuid) {
        RequestResponse response = RequestHandler.get("api/report/suspect/%s/active", suspectUuid.toString());
        if (!response.wasSuccessful() || response.asObject() == null) return Optional.empty();
        return Optional.of(Report.fromJson(response.asObject()));
    }

    // ── Claim ──────────────────────────────────────────────────────────────────

    /** Claims the highest-priority unclaimed report for this staff member. */
    public Optional<Report> claimNext(UUID staffUuid, String server) {
        JsonObject body = new JsonObject();
        body.addProperty("staffUuid", staffUuid.toString());
        body.addProperty("server", server);
        body.addProperty("source", SOURCE_GAME);

        RequestResponse response = RequestHandler.post("api/report/claim", body);
        if (!response.wasSuccessful() || response.asObject() == null) return Optional.empty();

        Report report = Report.fromJson(response.asObject());
        handling.put(staffUuid, report);
        return Optional.of(report);
    }

    /** Claims one specific report by id — used by the website, where staff pick from a list. */
    public Optional<Report> claimSpecific(String reportId, UUID staffUuid, String server) {
        JsonObject body = new JsonObject();
        body.addProperty("staffUuid", staffUuid.toString());
        body.addProperty("server", server);
        body.addProperty("source", SOURCE_GAME);

        RequestResponse response = RequestHandler.post("api/report/%s/claim", body, reportId);
        if (!response.wasSuccessful() || response.asObject() == null) return Optional.empty();

        Report report = Report.fromJson(response.asObject());
        handling.put(staffUuid, report);
        return Optional.of(report);
    }

    // ── Resolve / release ──────────────────────────────────────────────────────

    /** Resolves the report (e.g. {@code ACCEPTED}, {@code REJECTED}, {@code ABUSIVE}). */
    public boolean resolve(String reportId, UUID staffUuid, ReportStatus status, String reasonDetail) {
        JsonObject body = new JsonObject();
        body.addProperty("staffUuid", staffUuid.toString());
        body.addProperty("status", status.name());
        body.addProperty("reasonDetail", reasonDetail);
        body.addProperty("source", SOURCE_GAME);

        RequestResponse response = RequestHandler.post("api/report/%s/resolve", body, reportId);
        if (response.wasSuccessful()) {
            handling.remove(staffUuid);
            return true;
        }
        return false;
    }

    /** Puts the report back in the queue without resolving it. */
    public void release(String reportId, UUID staffUuid) {
        handling.remove(staffUuid);
        JsonObject body = new JsonObject();
        body.addProperty("source", SOURCE_GAME);
        Tasks.runAsync(() -> RequestHandler.post("api/report/%s/release", body, reportId));
    }

    /** Forgets this JVM's cached report for a staff member without touching the claim (e.g. they switched servers). */
    public void forgetLocal(UUID staffUuid) {
        handling.remove(staffUuid);
    }

    // ── Web-driven engagement (ReportEngagePacket) ─────────────────────────────

    /**
     * Blocking.  Loads the report the website just assigned to {@code staffUuid}, and if it
     * really is still theirs, caches it and hands it to the Paper-side {@link #engageHandler}.
     */
    public void engageLocally(UUID staffUuid, String reportId) {
        Optional<Report> loaded = getById(reportId);
        if (loaded.isEmpty()) return;

        Report report = loaded.get();
        if (report.getStatus() != ReportStatus.IN_PROGRESS || !staffUuid.toString().equals(report.getHandler())) {
            return; // released or reassigned again before we got here
        }

        handling.put(staffUuid, report);
        if (engageHandler != null) engageHandler.accept(staffUuid, report);
    }

    // ── Local cache (mutated by ReportUpdatedPacket) ────────────────────────────

    public void cacheHandling(UUID staffUuid, Report report) {
        handling.put(staffUuid, report);
    }

    /**
     * Drops any cached entry for this report (and for {@code handlerUuid}).
     *
     * @return the staff members on this JVM who were holding the report, so the caller
     *         can tell them it changed underneath them
     */
    public List<UUID> evictLocalHandling(String reportId, String handlerUuid) {
        List<UUID> evicted = new ArrayList<>();
        handling.entrySet().removeIf(entry -> {
            boolean match = reportId != null && reportId.equals(entry.getValue().getId());
            if (match) evicted.add(entry.getKey());
            return match;
        });
        if (handlerUuid != null && !handlerUuid.isBlank()) {
            try {
                handling.remove(UUID.fromString(handlerUuid));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return evicted;
    }

    /** Quiet one-line ping to online staff that a fresh report just entered the queue. */
    public void notifyNewReport(String reportId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(STAFF_PERMISSION)) {
                player.sendMessage(CC.notice("New report in the queue.", "Run */reporthandle* to pick it up."));
            }
        }
    }

    // ── Async convenience ────────────────────────────────────────────────────────

    public void create(UUID reporterUuid, String reporterServer, UUID suspectUuid, ReportCategory category,
                        List<ReportMessage> chatMessages, Consumer<Optional<Report>> callback) {
        Tasks.runAsync(() -> callback.accept(create(reporterUuid, reporterServer, suspectUuid, category, chatMessages)));
    }

    public void claimNext(UUID staffUuid, String server, Consumer<Optional<Report>> callback) {
        Tasks.runAsync(() -> callback.accept(claimNext(staffUuid, server)));
    }
}
