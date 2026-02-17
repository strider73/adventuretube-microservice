package com.adventuretube.auth.config.security;

import com.adventuretube.auth.filter.JwtWebFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

import static com.adventuretube.auth.config.security.SecurityConstants.OPEN_ENDPOINTS;


/**
 * Security filter chain for endpoints under /auth/**
 * - Applies only to /auth/** routes.
 * - Public access for login, registration, token refresh, logout, and Swagger-related paths.
 * - ADMIN role required for any other /auth/** endpoints.
 * - Integrates a custom JwtWebFilter to process JWT tokens before default auth mechanisms.
 */
@Configuration
@EnableWebFluxSecurity
@AllArgsConstructor
public class AuthServiceConfig {

    private final JwtWebFilter jwtWebFilter;

    @Bean
    public SecurityWebFilterChain apiFilterChain(ServerHttpSecurity httpSecurity) {
        httpSecurity
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/auth/**"))
            .authorizeExchange(authorize -> authorize
                    .pathMatchers(OPEN_ENDPOINTS).permitAll()
                    .anyExchange().hasRole("ADMIN")
            )
            .addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            ReactiveUserDetailsService reactiveUserDetailsService,
            PasswordEncoder passwordEncoder) {
        return authentication -> {
            String email = authentication.getName();
            String password = authentication.getCredentials().toString();

            return reactiveUserDetailsService.findByUsername(email)
                    .filter(userDetails -> passwordEncoder.matches(password, userDetails.getPassword()))
                    .map(userDetails -> (org.springframework.security.core.Authentication)
                            new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities()))
                    .switchIfEmpty(Mono.error(
                            new org.springframework.security.authentication.BadCredentialsException("Invalid username or password")));
        };
    }
}
