package com.adventuretube.apigateway.config;

import com.adventuretube.apigateway.exception.JwtTokenNotExistException;
import com.adventuretube.apigateway.service.JwtUtil;
import com.adventuretube.common.error.RestAPIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RefreshScope
@AllArgsConstructor
@Component
@Slf4j
public class AuthenticationFiter implements GatewayFilter {
    private final RouterValidator validator;
    private final JwtUtil jwtUtils;

    @Operation(summary = "Authenticate user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = RestAPIResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))
    })
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Step 1: Check if the request targets a secured endpoint
        if (validator.isSecured.test(request)) {
            log.info("request need a valid token");

            // Step 2: If the Authorization header is missing, throw an exception
            if (authMissing(request)) {
                throw new RuntimeException("Authorization header is missing");
            }
            // Step 3: Extract token from Authorization header
            final String token = request.getHeaders().getOrEmpty("Authorization").get(0);
            if( token == null || token.length() == 0){
                throw  new JwtTokenNotExistException("Token is not exist");
            }
            // Step 4: Validate the token (signature, expiration, claims)
            var claims = jwtUtils.getClaims(token);
            log.info("✅ Token validated successfully");
            log.info("📦 JWT Claims: {}", claims);
            // You can also log individual claims if needed:
            log.info("User ID: {}", claims.get("id"));
            log.info("User Role: {}", claims.get("role"));
            log.info("Token has been validate successfully !!!!");


        }
        log.info("request doesn't need a valid token");
        // Step 5: Continue the  Gateway filter chain Not Spring security filter  if everything is valid
        return chain.filter(exchange);
    }

    //    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus, String message) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(httpStatus);
//        response.getHeaders().add("Error-Message", message);
//        return response.setComplete();
//    }
//
    // Helper method to check for missing Authorization header
    private boolean authMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }
}
