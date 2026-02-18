package com.adventuretube.geospatial.filter;

import com.adventuretube.geospatial.service.JwtUtil;
import com.adventuretube.geospatial.service.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailService customUserDetailService;

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/register",
            "/auth/login",
            "/web/registerMember",
            "/actuator/health",
            "/healthcheck",
            "/swagger-ui.html",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (OPEN_ENDPOINTS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        log.info("JwtAuthFilter.filter has been called");

        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (token == null) {
            return chain.filter(exchange);
        }

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return chain.filter(exchange);
            }

            log.info("user name is: {} hasn't been authenticated yet", username);

            return customUserDetailService.findByUsername(username)
                    .flatMap(userDetails -> {
                        if (jwtUtil.validateToken(token, userDetails)) {
                            UsernamePasswordAuthenticationToken authenticationToken =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authenticationToken));
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        log.error("token error: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
        } catch (Exception e) {
            log.error("token error: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }
}
