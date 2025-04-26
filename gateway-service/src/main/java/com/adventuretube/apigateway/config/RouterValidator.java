package com.adventuretube.apigateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
public class RouterValidator {

    // List of public (non-secured) endpoint patterns
    public static final List<String> openEndPoints = List.of(
            "^/auth/users.*",             // signup
            "^/auth/token.*",              // ✅ login, refresh, revoke
            "^/web/registerMember.*",      // web signup (member service)
            "^/actuator/health.*",          // actuator health check
            "^/healthcheck.*",              // custom healthcheck

        // Swagger & OpenAPI
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
