package com.adventuretube.common.config;

import com.adventuretube.common.client.ServiceClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker configuration for inter-service communication.
 *
 * Key behavior:
 * - Only 5xx errors and network failures trip the circuit breaker
 * - 4xx errors (USER_NOT_FOUND, DUPLICATE, etc.) are business errors and do NOT trip it
 * - Timeout: 5 seconds per call
 * - Circuit opens after 50% failure rate in a 10-call sliding window
 * - Stays open for 30 seconds before testing with 3 half-open calls
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id ->
                new Resilience4JConfigBuilder(id)
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                                .slidingWindowSize(10)
                                .minimumNumberOfCalls(5)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofSeconds(30))
                                .permittedNumberOfCallsInHalfOpenState(3)
                                .recordException(e -> {
                                    // Only count 5xx and network errors as failures
                                    // 4xx errors are business logic (user not found, duplicate, etc.)
                                    if (e instanceof ServiceClientException sce) {
                                        return sce.getHttpStatus() >= 500;
                                    }
                                    return true; // Network errors, timeouts always count
                                })
                                .build())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(5))
                                .build())
                        .build()
        );
    }
}
