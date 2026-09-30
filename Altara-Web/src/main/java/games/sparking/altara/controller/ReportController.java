package games.sparking.altara.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.report.ReportCategory;
import games.sparking.altara.report.ReportGroup;
import games.sparking.altara.report.ReportStatus;
import games.sparking.altara.report.RejectionType;
import games.sparking.altara.service.ReportWebService;
import games.sparking.altara.service.ReportWebService.EngageOutcome;
import games.sparking.altara.service.ReportWebService.EngageResult;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static games.sparking.altara.controller.Responses.*;

/**
 * Report API — backs both the in-game commands and the external staff website.
 *
 * <h2>Queue &amp; history (website)</h2>
 * <pre>
 *   GET    /api/report/queue                   {pending: [...], inProgress: [...]}, pending sorted by priority,
 *                                              each with priority, suspectOnline, suspectServer, handlerOnline
 *   GET    /api/report?status=&amp;group=&amp;suspect=&amp;handler=&amp;before=&amp;limit=
 *                                              history/search, newest first; status is comma-separated,
 *                                              before is a createdAt cursor, limit defaults to 50 (max 200)
 *   GET    /api/report/{id}                    one report
 *   GET    /api/report/unhandled               raw pending + in-progress (unsorted, no sweep)
 *   GET    /api/report/handler/{uuid}          the report this staff member is handling (nullable)
 *   GET    /api/report/suspect/{uuid}/active   the active report against a suspect (nullable)
 * </pre>
 *
 * <h2>Handling</h2>
 * <pre>
 *   POST   /api/report/{id}/engage             {staffUuid, force?}  claim + pull them in-game (staff mode,
 *                                              report panel, follow the suspect) — any report group
 *   POST   /api/report/engage-next             {staffUuid, force?}  same, for the top of the queue
 *   POST   /api/report/{id}/claim              {staffUuid, server}  claim without any in-game action
 *                                              (e.g. a chat report handled entirely on the website)
 *   POST   /api/report/claim                   {staffUuid, server}  claim next, no in-game action (/reporthandle)
 *   POST   /api/report/{id}/heartbeat          {staffUuid}          keep a website-held claim from going stale
 *   POST   /api/report/{id}/resolve            {staffUuid, status, reasonDetail}
 *   POST   /api/report/{id}/release            {}                   back into the queue
 * </pre>
 *
 * Engage responses are always {@code {"outcome": "...", "report": {...}}} ({@code report}
 * is left out when there isn't one):
 * {@code ENGAGED} / {@code EMPTY} → 200, {@code NOT_FOUND} → 404, and {@code STAFF_OFFLINE},
 * {@code TAKEN}, {@code CLOSED}, {@code BUSY} → 409.  {@code BUSY} carries the report the
 * staff member is already holding; resend with {@code "force": true} to release it and switch.
 *
 * <h2>Metadata</h2>
 * <pre>
 *   GET    /api/report/categories              ReportCategory metadata
 *   GET    /api/report/rejection-types         RejectionType metadata
 *   POST   /api/report                         submit (merges onto an existing pending report)
 * </pre>
 *
 * Mutating calls accept an optional {@code "source"}; in-game calls send {@code "game"}, and
 * anything else is treated as the website — which is what makes an in-game handler get told
 * when their report is closed or released from the web panel.
 */
@Controller("/api/report")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class ReportController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final ReportWebService reportWebService;

    // ── Submit ─────────────────────────────────────────────────────────────────

    @Post
    public HttpResponse<String> submit(@Body String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            validateSubmit(json);
            return reportWebService.create(json)
                    .map(Responses::created)
                    .orElseGet(() -> serverError("Failed to persist report"));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Queue / history ────────────────────────────────────────────────────────

    @Get("/queue")
    public HttpResponse<String> queue() {
        return ok(reportWebService.queue());
    }

    @Get
    public HttpResponse<String> search(@QueryValue @Nullable String status,
                                       @QueryValue @Nullable String group,
                                       @QueryValue @Nullable String suspect,
                                       @QueryValue @Nullable String handler,
                                       @QueryValue @Nullable Long before,
                                       @QueryValue @Nullable Integer limit) {
        try {
            List<String> statuses = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                for (String part : status.split(",")) {
                    statuses.add(ReportStatus.valueOf(part.trim().toUpperCase()).name());
                }
            }
            String groupName = group == null || group.isBlank() ? null : ReportGroup.valueOf(group.trim().toUpperCase()).name();
            String suspectUuid = suspect == null || suspect.isBlank() ? null : UUID.fromString(suspect.trim()).toString();
            String handlerUuid = handler == null || handler.isBlank() ? null : UUID.fromString(handler.trim()).toString();
            int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, limit));

            return ok(reportWebService.search(statuses, groupName, suspectUuid, handlerUuid, before, size));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Retrieve ───────────────────────────────────────────────────────────────

    @Get("/{id}")
    public HttpResponse<String> getReport(String id) {
        return reportWebService.getById(id)
                .map(Responses::ok)
                .orElseGet(() -> notFound("Report not found: " + id));
    }

    @Get("/unhandled")
    public HttpResponse<String> unhandled() {
        return ok(reportWebService.listUnhandled());
    }

    @Get("/handler/{uuid}")
    public HttpResponse<String> getHandling(UUID uuid) {
        return reportWebService.getByHandler(uuid.toString())
                .map(Responses::ok)
                .orElseGet(() -> ok("null"));
    }

    @Get("/suspect/{uuid}/active")
    public HttpResponse<String> getActiveForSuspect(UUID uuid) {
        return reportWebService.getActiveForSuspect(uuid.toString())
                .map(Responses::ok)
                .orElseGet(() -> ok("null"));
    }

    // ── Engage (website → in-game) ─────────────────────────────────────────────

    @Post("/{id}/engage")
    public HttpResponse<String> engage(String id, @Body String body) {
        try {
            JsonObject json = parse(body);
            String staffUuid = requireUuid(json, "staffUuid");
            return engageResponse(reportWebService.engage(id, staffUuid, bool(json, "force")));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Post("/engage-next")
    public HttpResponse<String> engageNext(@Body String body) {
        try {
            JsonObject json = parse(body);
            String staffUuid = requireUuid(json, "staffUuid");
            return engageResponse(reportWebService.engageNext(staffUuid, bool(json, "force")));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    private static HttpResponse<String> engageResponse(EngageResult result) {
        JsonObject body = new JsonObject();
        body.addProperty("outcome", result.outcome().name());
        body.add("report", result.report());

        HttpStatus status = switch (result.outcome()) {
            case ENGAGED, EMPTY -> HttpStatus.OK;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case STAFF_OFFLINE, TAKEN, CLOSED, BUSY -> HttpStatus.CONFLICT;
        };
        return withStatus(status, body);
    }

    // ── Claim (no in-game action) ──────────────────────────────────────────────

    @Post("/claim")
    public HttpResponse<String> claimNext(@Body String body) {
        try {
            JsonObject json = parse(body);
            String staffUuid = requireUuid(json, "staffUuid");
            requireField(json, "server");

            return reportWebService.claimNext(staffUuid, json.get("server").getAsString(), source(json))
                    .map(Responses::ok)
                    .orElseGet(() -> ok("null"));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Post("/{id}/claim")
    public HttpResponse<String> claimSpecific(String id, @Body String body) {
        try {
            JsonObject json = parse(body);
            String staffUuid = requireUuid(json, "staffUuid");
            requireField(json, "server");

            return reportWebService.claimSpecific(id, staffUuid, json.get("server").getAsString(), source(json))
                    .map(Responses::ok)
                    .orElseGet(() -> conflict("Report is no longer available to claim: " + id));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Post("/{id}/heartbeat")
    public HttpResponse<String> heartbeat(String id, @Body String body) {
        try {
            String staffUuid = requireUuid(parse(body), "staffUuid");
            return reportWebService.heartbeat(id, staffUuid)
                    ? ok("{\"ok\":true}")
                    : conflict("You are no longer handling report " + id);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    // ── Resolve / release ──────────────────────────────────────────────────────

    @Post("/{id}/resolve")
    public HttpResponse<String> resolve(String id, @Body String body) {
        try {
            JsonObject json = parse(body);
            String staffUuid = requireUuid(json, "staffUuid");
            requireField(json, "status");
            ReportStatus status = ReportStatus.valueOf(json.get("status").getAsString());
            if (!status.isTerminal()) {
                throw new IllegalArgumentException("status must be a terminal status (ACCEPTED, REJECTED, ABUSIVE, EXPIRED)");
            }

            String reasonDetail = json.has("reasonDetail") && !json.get("reasonDetail").isJsonNull()
                    ? json.get("reasonDetail").getAsString() : null;

            return reportWebService.resolve(id, staffUuid, status.name(), reasonDetail, source(json))
                    .map(Responses::ok)
                    .orElseGet(() -> notFound("Report not found: " + id));
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @Post("/{id}/release")
    @Consumes(MediaType.ALL)
    public HttpResponse<String> release(String id, @Body @Nullable String body) {
        String source = null;
        try {
            source = source(parse(body));
        } catch (RuntimeException ignored) {
            // No/invalid body — treat as the website.
        }
        return reportWebService.release(id, source)
                .map(Responses::ok)
                .orElseGet(() -> notFound("Report not found: " + id));
    }

    // ── Enum metadata (website dropdowns) ──────────────────────────────────────

    @Get("/categories")
    public HttpResponse<String> listCategories() {
        JsonArray array = new JsonArray();
        for (ReportCategory category : ReportCategory.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", category.name());
            obj.addProperty("group", category.getGroup().name());
            obj.addProperty("displayName", category.getDisplayName());
            obj.addProperty("description", category.getDescription());
            array.add(obj);
        }
        return ok(array);
    }

    @Get("/rejection-types")
    public HttpResponse<String> listRejectionTypes() {
        JsonArray array = new JsonArray();
        for (RejectionType type : RejectionType.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", type.name());
            obj.addProperty("abusive", type.isAbusive());
            obj.addProperty("displayName", type.getDisplayName());
            obj.addProperty("description", type.getDescription());
            array.add(obj);
        }
        return ok(array);
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private static void validateSubmit(JsonObject json) {
        requireUuid(json, "suspectUuid");
        requireField(json, "category");
        ReportCategory.valueOf(json.get("category").getAsString());

        if (json.has("reporterUuid") && !json.get("reporterUuid").isJsonNull()) {
            UUID.fromString(json.get("reporterUuid").getAsString());
        }
    }

    private static JsonObject parse(String body) {
        if (body == null || body.isBlank()) return new JsonObject();
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private static void requireField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
    }

    /** A required UUID field, normalised to its canonical string form. */
    private static String requireUuid(JsonObject json, String field) {
        requireField(json, field);
        return UUID.fromString(json.get(field).getAsString()).toString();
    }

    private static boolean bool(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull() && json.get(field).getAsBoolean();
    }

    private static String source(JsonObject json) {
        return json.has("source") && !json.get("source").isJsonNull() ? json.get("source").getAsString() : null;
    }
}
