package com.adventuretube.config;

import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class TracingFilterConfig {

    @Bean
    public ObservationPredicate skipActuatorTracing() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                String path = serverContext.getCarrier().getRequestURI();
                return !path.startsWith("/actuator");
            }
            return true;
        };
    }
}
