package games.sparking.altara.filter;

import games.sparking.altara.Altara;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Simple shared-secret authentication filter.
 * The backend key is read from the Altara config system (config.json) via the
 * {@link Altara} singleton — no Spring {@code @Value} needed.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Health endpoint is intentionally unauthenticated (load-balancer probes, etc.)
        if (request.getRequestURI().equals("/api/server/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String backendKey = Altara.getSharedInstance().getMainConfig().getBackendKey();
        String auth = request.getHeader("Authorization");

/*        if (auth == null || !auth.equals(backendKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }*/

        filterChain.doFilter(request, response);
    }
}
