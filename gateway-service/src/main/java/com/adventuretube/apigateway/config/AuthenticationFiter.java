package com.adventuretube.apigateway.config;

import com.adventuretube.apigateway.service.JwtUtil;
import com.adventuretube.common.api.response.ServiceResponse;
import io.jsonwebtoken.Claims;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
@RefreshScope
@AllArgsConstructor
@Component
@Slf4j
public class AuthenticationFiter implements GatewayFilter {
    private final RouterValidator validator;
    private final JwtUtil jwtUtils;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Authenticate user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ServiceResponse.class)))
    })
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Step 1: Check if the request targets a secured endpoint
        if (validator.isSecured.test(request)) {
            log.info("request need a valid token");

            try {
                if (authMissing(request)) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "Authorization header is missing", "TOKEN_MISSING");
                }

                final String token = request.getHeaders().getOrEmpty("Authorization").get(0);
                if (token == null || token.length() == 0) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "Token is not exist", "TOKEN_MISSING");
                }

                Claims claims;
                String path = request.getURI().getRawPath();
                //here all refresh and access token expiration check will be done
                claims = jwtUtils.getClaims(token);
                log.info("✅ Token validated successfully");
                log.info("User ID: {}", claims.get("id"));
                log.info("User Role: {}", claims.get("role"));

            } catch (ExpiredJwtException e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Expired JWT token: gateway-service", "TOKEN_EXPIRED");
            } catch (SignatureException e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT signature: gateway-service",
                        "TOKEN_INVALID_SIGNATURE");
            } catch (MalformedJwtException e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Malformed JWT token: gateway-service",
                        "TOKEN_MALFORMED");
            } catch (Exception e) {
                return onError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: gateway-service",
                        "INTERNAL_ERROR");
            }


        }else{
            log.info("request doesn't need a valid token");
        }
        // Step 5: Continue the  Gateway filter chain Not Spring security filter  if everything is valid
        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message, String errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);


        ServiceResponse<Void> serviceResponse = ServiceResponse.<Void>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(serviceResponse);
        } catch (Exception e) {
            bytes = "{}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }


    // Helper method to check for missing Authorization header
    private boolean authMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }
}
