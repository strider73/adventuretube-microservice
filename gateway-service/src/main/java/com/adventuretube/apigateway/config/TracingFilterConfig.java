package com.adventuretube.apigateway.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
