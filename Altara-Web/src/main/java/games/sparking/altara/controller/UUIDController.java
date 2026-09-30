package games.sparking.altara.controller;

import games.sparking.altara.service.UUIDWebService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.UUID;

import static games.sparking.altara.controller.Responses.notFound;

/**
 * UUID ↔ name resolution from stored profiles.
 *
 * <pre>
 *   GET /api/uuid/name/{name}   {"uuid", "name"} or 404
 *   GET /api/uuid/{uuid}        {"uuid", "name"} or 404
 * </pre>
 */
@Controller("/api/uuid")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UUIDController {

    private final UUIDWebService uuidWebService;

    @Get("/name/{name}")
    public HttpResponse<String> getByName(String name) {
        return uuidWebService.resolveNameToJson(name.toLowerCase(Locale.ROOT))
                .map(Responses::ok)
                .orElseGet(() -> notFound("No profile found for name: " + name));
    }

    @Get("/{uuid}")
    public HttpResponse<String> getByUuid(UUID uuid) {
        return uuidWebService.resolveUuidToJson(uuid.toString())
                .map(Responses::ok)
                .orElseGet(() -> notFound("No profile found for uuid: " + uuid));
    }
}
