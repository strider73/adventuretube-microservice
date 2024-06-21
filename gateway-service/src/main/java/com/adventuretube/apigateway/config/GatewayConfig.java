package com.adventuretube.apigateway.config;


import lombok.AllArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@AllArgsConstructor
@Configuration
public class GatewayConfig {
    private AuthenticationFiter filter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder){
        return builder.routes()
                .route("member-service",r -> r.path("/member/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://member-service"))
                .route("web-service", r -> r.path("/web/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://web-service"))
                .route("security-service", r -> r.path("/auth/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://auth-service"))
                .route("geospatial-service", r -> r.path("/geo/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://geospatial-service"))
                .build();
    }
}
