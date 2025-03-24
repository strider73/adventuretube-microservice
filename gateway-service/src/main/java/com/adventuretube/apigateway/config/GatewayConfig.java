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

        // Swagger docs (❗ without filters)
        .route("auth-docs", r -> r.path("/auth/v3/api-docs").uri("lb://auth-service"))
        .route("member-docs", r -> r.path("/member/v3/api-docs").uri("lb://member-service"))
        .route("geo-docs", r -> r.path("/geo/v3/api-docs").uri("lb://geospatial-service"))
        .route("web-docs", r -> r.path("/web/v3/api-docs").uri("lb://web-service"))

        // API routes (with filters)
        .route("auth-service", r -> r.path("/auth/**").filters(f -> f.filter(filter)).uri("lb://auth-service"))
        .route("member-service", r -> r.path("/member/**").filters(f -> f.filter(filter)).uri("lb://member-service"))
        .route("geo-service", r -> r.path("/geo/**").filters(f -> f.filter(filter)).uri("lb://geospatial-service"))
        .route("web-service", r -> r.path("/web/**").filters(f -> f.filter(filter)).uri("lb://web-service"))

        .build();
    }
}
