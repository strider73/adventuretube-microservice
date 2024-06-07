package com.adventuretube.apigateway.config;

import com.adventuretube.apigateway.service.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RefreshScope
@AllArgsConstructor
@Component
public class AuthenticationFiter implements GatewayFilter {
    private final RouterValidator validator;
    private final JwtUtil jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //check the request is secured
        //if yes
        if (validator.isSecured.test(request)) {
            if (authMissing(request)) {//check Authentication header
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
            //if the authentication header exist , extract the token
            final String token = request.getHeaders().getOrEmpty("Authorization").get(0);
            try{
                jwtUtils.validateToken(token);
            }catch (Exception e){
                System.out.println("invalid access...!");
                throw new RuntimeException("un authorized access to application");
            }
           //and check the expiration
            if(jwtUtils.isExpired(token)){
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

        }
        //if every thing ok continue filter chain
        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private boolean authMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }
}
