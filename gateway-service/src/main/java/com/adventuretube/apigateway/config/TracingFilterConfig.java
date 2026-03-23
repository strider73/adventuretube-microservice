package com.adventuretube.apigateway.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class TracingFilterConfig {

    @Bean
    public ObservationPredicate skipActuatorTracing() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                String path = serverContext.getCarrier().getURI().getPath();
                return !path.startsWith("/actuator");
            }
            return true;
        };
    }

    /**
     * Customizes the span name to include the request path.
     * Default: "http get" → Custom: "http get /auth/token/refresh"
     * This makes Zipkin traces identifiable by endpoint.
     */
    @Bean
    public DefaultServerRequestObservationConvention serverRequestObservationConvention() {
        return new DefaultServerRequestObservationConvention() {
            @Override
            public String getName() {
                return "http.server.requests";
            }

            @Override
            public String getContextualName(ServerRequestObservationContext context) {
                ServerHttpRequest request = context.getCarrier();
                String method = request.getMethod().name().toLowerCase();
                String path = request.getURI().getPath();
                return "http " + method + " " + path;
            }
        };
    }
}
