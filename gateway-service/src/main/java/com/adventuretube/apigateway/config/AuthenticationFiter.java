package com.adventuretube.apigateway.config;

import com.adventuretube.apigateway.service.JwtUtil;
import com.adventuretube.common.error.RestAPIErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.SignatureException;

@RefreshScope
@AllArgsConstructor
@Component
@Slf4j
public class AuthenticationFiter implements GatewayFilter {
    private final RouterValidator validator;
    private final JwtUtil jwtUtils;

    @Operation(summary = "Authenticate user")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //check the request is secured
        //if yes
        if (validator.isSecured.test(request)) {
            if (authMissing(request)) {//check Authentication header
                throw new RuntimeException("Authorization header is missing");
            }
            //if the authentication header exist , extract the token
            final String token = request.getHeaders().getOrEmpty("Authorization").get(0);

            jwtUtils.validateToken(token);
            System.out.println("Token has been validate successfully !!!!");


        }
        //if every thing ok continue filter chain
        return chain.filter(exchange);
    }

    //    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus, String message) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(httpStatus);
//        response.getHeaders().add("Error-Message", message);
//        return response.setComplete();
//    }
//
    private boolean authMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }
}
