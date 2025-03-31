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
                .route("auth-docs", r -> r.path("/auth-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))// 👈 this removes "/auth-service"
                        .uri("lb://auth-service"))
                .route("member-docs", r -> r.path("/member-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))// 👈 this removes "/member-service"
                        .uri("lb://member-service"))
                .route("web-docs", r -> r.path("/web-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))// 👈 this removes "/web-service"
                        .uri("lb://web-service"))
                .route("geo-docs", r -> r.path("/geo-service/v3/api-docs")
                        .filters(f -> f.stripPrefix(1))// 👈 this removes "/geo-service"
                        .uri("lb://geospatial-service"))


                // API routes (with filters)
        .route("auth-service", r -> r.path("/auth/**").filters(f -> f.filter(filter)).uri("lb://auth-service"))
        .route("member-service", r -> r.path("/member/**").filters(f -> f.filter(filter)).uri("lb://member-service"))
        .route("geo-service", r -> r.path("/geo/**").filters(f -> f.filter(filter)).uri("lb://geospatial-service"))
        .route("web-service", r -> r.path("/web/**").filters(f -> f.filter(filter)).uri("lb://web-service"))

        .build();
    }
}
