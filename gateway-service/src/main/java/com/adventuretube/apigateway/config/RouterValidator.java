package com.adventuretube.apigateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

/**
 * RouterValidator determines which API endpoints require JWT authentication.
 *
 * <p><b>Open Endpoints (No JWT Required):</b></p>
 * <ul>
 *   <li>{@code /auth/users} - User registration (authenticates via Google ID token)</li>
 *   <li>{@code /auth/token} - Login (authenticates via Google ID token, issues JWT)</li>
 *   <li>{@code /web/registerMember} - Web signup</li>
 *   <li>{@code /actuator/health}, {@code /healthcheck} - Health checks</li>
 *   <li>Swagger/OpenAPI documentation endpoints</li>
 * </ul>
 *
 * <p><b>Secured Endpoints (JWT Required):</b></p>
 * <ul>
 *   <li>{@code /auth/token/refresh} - Requires valid refresh token</li>
 *   <li>{@code /auth/token/revoke} - Requires valid access token</li>
 *   <li>All other endpoints not listed in openEndPoints</li>
 * </ul>
 *
 * <p><b>IMPORTANT:</b> The pattern {@code ^/auth/token$} matches ONLY the exact path
 * {@code /auth/token} (login). Sub-paths like {@code /auth/token/refresh} and
 * {@code /auth/token/revoke} are NOT matched, so they require JWT validation.</p>
 */
@Service
public class RouterValidator {

    /**
     * List of public endpoint patterns that bypass JWT authentication.
     * Uses regex patterns - requests matching these patterns skip the AuthenticationFilter.
     */
    public static final List<String> openEndPoints = List.of(
            // === Auth Service ===
            "^/auth/users.*",              // POST: User registration (uses Google ID token, not JWT)
            "^/auth/token$",               // POST: Login only (uses Google ID token to obtain JWT)
                                           // NOTE: /auth/token/refresh and /auth/token/revoke are SECURED (require JWT)

            // === Member Service ===
            "^/web/registerMember.*",      // Web signup endpoint

            // === Health Checks ===
            "^/actuator/health.*",         // Spring Actuator health endpoint
            "^/healthcheck.*",             // Custom health check endpoint

            // === Swagger & OpenAPI Documentation ===
            "^/swagger-ui\\.html.*",
            "^/swagger-ui/.*",
            "^/auth-service/v3/api-docs.*",
            "^/member-service/v3/api-docs.*",
            "^/web-service/v3/api-docs.*",
            "^/geo-service/v3/api-docs.*"
    );

    // Predicate to check if request requires authentication
    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getRawPath();
        System.out.println("🔍 Securing request path: " + path);
        return openEndPoints.stream().noneMatch(pattern -> path.matches(pattern));
    };
}
