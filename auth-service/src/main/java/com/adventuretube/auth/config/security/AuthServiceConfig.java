package com.adventuretube.auth.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableWebFluxSecurity
public class AuthServiceConfig {

    @Bean
    public SecurityWebFilterChain apiFilterChain(ServerHttpSecurity httpSecurity) {
        // All endpoint protection is handled by Gateway (RouterValidator).
        // Auth-service permits all requests that reach it.
        httpSecurity
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(authorize -> authorize
                .anyExchange().permitAll()
            );
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
                    .filterWhen(userDetails ->
                            Mono.fromCallable(() -> passwordEncoder.matches(password, userDetails.getPassword()))
                                    .subscribeOn(Schedulers.boundedElastic()))
                    .map(userDetails -> (Authentication)
                            new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities()))
                    .switchIfEmpty(Mono.error(
                            new BadCredentialsException("Invalid username or password")));
        };
    }
}
