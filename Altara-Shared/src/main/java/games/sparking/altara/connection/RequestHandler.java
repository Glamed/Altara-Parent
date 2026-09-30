package games.sparking.altara.connection;

import com.google.gson.JsonElement;
import games.sparking.altara.Altara;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.Timings;
import lombok.Getter;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RequestHandler {

    @Getter
    private static boolean apiDown = false;

    @Getter
    private static long lastError = -1;

    @Getter
    private static long lastRequest = -1;

    @Getter
    private static long lastLatency = 0;
    private static long averageLatency = 0;

    @Getter
    private static long totalRequests = 0;

    @Getter
    private static long averageLatencyTicks = 0;

    public static double getAverageLatency() {
        if (averageLatencyTicks == 0)
            return -1;

        return (averageLatency + 0.0D) / (averageLatencyTicks + 0.0D);
    }

    public static int getBackLogSize() {
        return backLog.size();
    }

    private static final List<BackLogEntry> backLog = new CopyOnWriteArrayList<>();

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5L))
            .writeTimeout(Duration.ofSeconds(5L))
            .readTimeout(Duration.ofSeconds(5L))
            .build();

    public static void startBackLogTask() {
        Tasks.runTimerAsync(RequestHandler::sendBackLog, 6000L, 6000L);
    }

    public static RequestResponse get(String endpoint, Object... args) {
        Request.Builder builder = newBuilder(endpoint, args);
        builder.get();

        return call(builder, false);
    }

    public static RequestResponse post(String endpoint, JsonElement body, Object... args) {
        Request.Builder builder = newBuilder(endpoint, args);
        builder.post(RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8")));
//        builder.post(RequestBody.create(body.toString(), MediaType.parse("application/json")));

        return call(builder, false);
    }

    public static RequestResponse put(String endpoint, JsonElement body, Object... args) {
        Request.Builder builder = newBuilder(endpoint, args);
        builder.put(RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8")));
//        builder.put(RequestBody.create(body.toString(), MediaType.parse("application/json")));

        return call(builder, false);
    }

    public static RequestResponse delete(String endpoint, Object... args) {
        Request.Builder builder = newBuilder(endpoint, args);
        builder.delete();

        return call(builder, false);
    }

    private static RequestResponse call(Request.Builder builder, boolean fromBackLog) {
        Timings timings = new Timings("api-request").startTimings();
        lastRequest = System.currentTimeMillis();
        totalRequests++;

        boolean newDown = false;
        try (Response response = CLIENT.newCall(builder.build()).execute()) {
            RequestResponse requestResponse = RequestResponse.ofResponse(response, builder);
            newDown = requestResponse.couldNotConnect();

            if (newDown)
                lastError = System.currentTimeMillis();
            else {
                lastLatency = timings.stopTimings().calculateDifference();
                averageLatency += timings.calculateDifference();
                averageLatencyTicks++;
            }
            return requestResponse;
        } catch (IOException e) {
            newDown = true;
            lastError = System.currentTimeMillis();
            e.printStackTrace();
            return RequestResponse.ofError(e, builder);
        } finally {
            // API just came back: replay queued requests off the calling thread.
            if (!newDown && apiDown && !fromBackLog)
                Tasks.runAsync(RequestHandler::sendBackLog);

            apiDown = newDown;
        }
    }

    private static Request.Builder newBuilder(String endpoint, Object... args) {
        Object[] encoded = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            encoded[i] = args[i] instanceof String string
                    ? URLEncoder.encode(string, StandardCharsets.UTF_8).replace("+", "%20")
                    : args[i];
        }

        return new Request.Builder()
                .url(Altara.getSharedInstance().getMainConfig().getBackendHost() + String.format(endpoint, encoded))
                .addHeader("Authorization", Altara.getSharedInstance().getMainConfig().getBackendKey());
    }

    public static void addToBackLog(BackLogEntry entry) {
        if (!apiDown)
            throw new UnsupportedOperationException("Cannot add requests to backlog while api is not down");

        backLog.add(entry);
    }

    public static synchronized void sendBackLog() {
        Iterator<BackLogEntry> iterator = backLog.iterator();
        while (iterator.hasNext()) {
            BackLogEntry next = iterator.next();
            RequestResponse response = call(next.getBuilder(), true);
            if (response.couldNotConnect()) return; // still down — keep the rest queued

            next.onSend(response);
            backLog.remove(next);
        }
    }

}