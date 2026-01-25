package com.adventuretube.common.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Common WebClient configuration for inter-service communication.
 *
 * The @LoadBalanced annotation enables service discovery via Eureka,
 * allowing services to call each other using logical service names
 * (e.g., "http://MEMBER-SERVICE") instead of hardcoded URLs.
 *
 * Usage in services:
 * <pre>
 * {@code
 * @Autowired
 * private WebClient.Builder webClientBuilder;
 *
 * WebClient webClient = webClientBuilder
 *     .baseUrl("http://MEMBER-SERVICE")
 *     .build();
 * }
 * </pre>
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
