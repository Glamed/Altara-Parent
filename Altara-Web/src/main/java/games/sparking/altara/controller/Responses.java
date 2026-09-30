package games.sparking.altara.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.utils.Statics;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;

/** JSON responses shared by every controller.  Bodies are already-serialised strings. */
final class Responses {

    private Responses() {}

    static MutableHttpResponse<String> ok(String json) {
        return json(HttpStatus.OK, json);
    }

    static MutableHttpResponse<String> ok(JsonElement json) {
        return ok(Statics.GSON.toJson(json));
    }

    static MutableHttpResponse<String> created(JsonElement json) {
        return json(HttpStatus.CREATED, Statics.GSON.toJson(json));
    }

    static MutableHttpResponse<String> notFound(String message) {
        return json(HttpStatus.NOT_FOUND, error(message));
    }

    static MutableHttpResponse<String> badRequest(String message) {
        return json(HttpStatus.BAD_REQUEST, error(message));
    }

    static MutableHttpResponse<String> serverError(String message) {
        return json(HttpStatus.INTERNAL_SERVER_ERROR, error(message));
    }

    private static MutableHttpResponse<String> json(HttpStatus status, String body) {
        return HttpResponse.<String>status(status).contentType(MediaType.APPLICATION_JSON_TYPE).body(body);
    }

    private static String error(String message) {
        JsonObject body = new JsonObject();
        body.addProperty("error", message == null ? "Unknown error" : message);
        return body.toString();
    }
}
