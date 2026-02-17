package com.adventuretube.auth.filter;

import com.adventuretube.auth.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static com.adventuretube.auth.config.security.SecurityConstants.OPEN_ENDPOINTS;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtWebFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final ReactiveUserDetailsService reactiveUserDetailsService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip filtering for open endpoints
        if (Arrays.stream(OPEN_ENDPOINTS).anyMatch(endpoint -> {
            if (endpoint.endsWith("/**")) {
                String prefix = endpoint.substring(0, endpoint.length() - 3);
                return path.startsWith(prefix);
            }
            return path.equals(endpoint);
        })) {
            if (!path.equals("/actuator/health")) {
                log.info("JWT Token validation will not progress for path: {}", path);
            }
            return chain.filter(exchange);
        }

        log.info("JWT Token validation in progress for path: {}", path);

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null) {
            return chain.filter(exchange);
        }

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return chain.filter(exchange);
            }

            return reactiveUserDetailsService.findByUsername(username)
                    .filter(userDetails -> jwtUtil.validateToken(token, userDetails))
                    .flatMap(userDetails -> {
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authenticationToken));
                    })
                    .switchIfEmpty(chain.filter(exchange));
        } catch (Exception e) {
            log.error("Error processing JWT token: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }
}
