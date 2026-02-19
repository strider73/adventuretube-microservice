package com.adventuretube.member;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class MemberServiceConfig {

    @Bean
    public SecurityWebFilterChain apiFilterChain(ServerHttpSecurity httpSecurity) {
        // All endpoint protection is handled by Gateway (RouterValidator).
        // Member-service permits all requests that reach it.
        httpSecurity
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(authorize -> authorize
                .anyExchange().permitAll()
            );
        return httpSecurity.build();
    }
}
