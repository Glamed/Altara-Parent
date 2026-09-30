package games.sparking.altara.filter;

import games.sparking.altara.Altara;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared-secret authentication: every request must carry the {@code backendKey} from
 * config.json in its {@code Authorization} header (Altara-Shared's {@code RequestHandler}
 * sends it automatically).  Only the health probe is public.
 */
@ServerFilter(ServerFilter.MATCH_ALL_PATTERN)
public class AuthFilter {

    private static final String HEALTH_PATH = "/api/server/health";

    /** Returns {@code null} to let the request through, or the 401 response. */
    @RequestFilter
    @Nullable
    public HttpResponse<?> authenticate(HttpRequest<?> request) {
        if (HEALTH_PATH.equals(request.getPath())) return null;

        String backendKey = Altara.getSharedInstance().getMainConfig().getBackendKey();
        String auth = request.getHeaders().get("Authorization");
        if (backendKey == null || backendKey.isBlank() || auth == null || !constantTimeEquals(auth, backendKey)) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON_TYPE)
                    .body("{\"error\":\"Unauthorized\"}");
        }
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
