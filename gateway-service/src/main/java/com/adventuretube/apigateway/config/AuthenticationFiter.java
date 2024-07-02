package com.adventuretube.apigateway.config;

import com.adventuretube.apigateway.exception.JwtTokenNotExistException;
import com.adventuretube.apigateway.service.JwtUtil;
import com.adventuretube.common.error.RestAPIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))
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
            if( token == null || token.length() == 0){
                throw  new JwtTokenNotExistException("Token is not exist");
            }
            //getClaim actually doing all basic validation
            //like signing expiration
            jwtUtils.getClaims(token);
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
