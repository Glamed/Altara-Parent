package games.sparking.altara.connection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Getter
public class RequestResponse {

    private final int code;
    private final JsonElement response;
    private final String errorMessage;
    private final Request.Builder requestBuilder;

    public static RequestResponse ofResponse(Response response, Request.Builder requestBuilder) {
        if (response.code() > 500) {
            return new RequestResponse(
                    response.code(),
                    null,
                    "Could not connect to API",
                    requestBuilder
            );
        }

        try {
            if (response.body() == null) {
                return new RequestResponse(response.code(), null, null, requestBuilder);
            }

            String raw = response.body().string();
            JsonElement body;
            try {
                body = raw.isBlank() ? JsonNull.INSTANCE : JsonParser.parseString(raw);
            } catch (JsonParseException e) {
                // Error pages (e.g. a proxy's HTML 500) aren't JSON — keep the status code.
                if (!response.isSuccessful()) {
                    return new RequestResponse(response.code(), null, "HTTP " + response.code(), requestBuilder);
                }
                throw e;
            }

            if (!response.isSuccessful()) {
                String message = "Unknown Error";

                if (body.isJsonObject()) {
                    JsonObject json = body.getAsJsonObject();

                    if (json.has("error") && json.get("error").isJsonPrimitive())
                        message = json.get("error").getAsString();

                    if (json.has("message") && json.get("message").isJsonPrimitive()
                            && !json.get("message").getAsString().isEmpty())
                        message = json.get("message").getAsString();
                }

                return new RequestResponse(response.code(), body, message, requestBuilder);
            }

            return new RequestResponse(response.code(), body, null, requestBuilder);
        } catch (JsonParseException | IOException e) {
            e.printStackTrace();
            return ofError(e, requestBuilder);
        }
    }

    public static RequestResponse ofError(Throwable throwable, Request.Builder requestBuilder) {
        int code = 0;
        String message = "Failed to get response from API: %s (%s)";

        if (throwable instanceof ConnectException
                || throwable instanceof UnknownHostException
                || throwable instanceof SocketTimeoutException) {
            code = -1;
            message = "Could not connect to API: %s";
        }

        if (throwable instanceof JsonParseException) {
            code = -2;
            message = "Could not parse response body: %s (%s)";
        }

        return new RequestResponse(
                code,
                null,
                String.format(message, throwable.getClass().getSimpleName(), throwable.getMessage()),
                requestBuilder
        );
    }

    private RequestResponse(int code, JsonElement response, String errorMessage, Request.Builder requestBuilder) {
        this.code = code;
        this.response = response;
        this.errorMessage = errorMessage;
        this.requestBuilder = requestBuilder;
    }

    public boolean wasSuccessful() {
        return code >= 200 && code < 300;
    }

    public boolean couldNotConnect() {
        return code >= 500 || code == -1;
    }

    public JsonObject asObject() {
        return response.getAsJsonObject();
    }

    public JsonArray asArray() {
        return response.getAsJsonArray();
    }

}