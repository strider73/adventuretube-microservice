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
            "/web/registerMember"

    );

    //isSecured is predicate that
    //checks URI path does not contain any of the open endpoint
    public Predicate<ServerHttpRequest> isSecured =
                     request -> openEndPoints.stream()
                             .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
