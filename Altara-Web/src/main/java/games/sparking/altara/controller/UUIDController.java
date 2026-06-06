package games.sparking.altara.controller;

import games.sparking.altara.service.UUIDWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for UUID ↔ name resolution.
 *
 * <pre>
 *   GET  /api/uuid/name/{name}   — resolve player name to UUID
 *   GET  /api/uuid/{uuid}        — resolve UUID to player name
 * </pre>
 *
 * Results are backed by the "profiles" MongoDB collection so they are
 * always consistent with the stored profile data.
 */
@RestController
@RequestMapping("/api/uuid")
@RequiredArgsConstructor
public class UUIDController {

    private final UUIDWebService uuidWebService;

    /**
     * Resolve a player name to their UUID.
     * Returns {@code {"uuid":"...", "name":"..."}} or 404.
     */
    @GetMapping(value = "/name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getByName(@PathVariable String name) {
        Optional<String> result = uuidWebService.resolveNameToJson(name);
        return result
                .map(json -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json))
                .orElse(notFound("No profile found for name: " + name));
    }

    /**
     * Resolve a UUID to the player's current name.
     * Returns {@code {"uuid":"...", "name":"..."}} or 404.
     */
    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getByUuid(@PathVariable UUID uuid) {
        Optional<String> result = uuidWebService.resolveUuidToJson(uuid);
        return result
                .map(json -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json))
                .orElse(notFound("No profile found for uuid: " + uuid));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ResponseEntity<String> notFound(String message) {
        return ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"" + message + "\"}");
    }
}

