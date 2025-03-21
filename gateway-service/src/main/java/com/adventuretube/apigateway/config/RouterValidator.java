package com.adventuretube.apigateway.config;


import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
public class RouterValidator {
    // list of path that do not require secuirty checks
    public static final List<String> openEndPoints = List.of(
           "/auth/register",
            "/auth/login",
            "/web/registerMember",
            "/actuator/health", // Allow health check endpoint
            "/healthcheck",      // Add any custom health check endpoints here
            "/swagger-ui.html",   // Allow Swagger UI
            "/swagger-ui/**",      // Allow Swagger static resources
            "/v3/api-docs/**",     // Allow OpenAPI documentation
            "/v3/api-docs"         // Allow direct API docs access

    );

    //isSecured is predicate that
    //checks URI path does not contain any of the open endpoint
    public Predicate<ServerHttpRequest> isSecured =
                     request -> openEndPoints.stream()
                             .noneMatch(uri -> request.getURI().getPath().startsWith(uri));
}
