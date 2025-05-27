package com.adventuretube.auth.config.security;

import com.adventuretube.auth.filter.JwtAuthFilter;
import com.adventuretube.auth.provider.CustomAuthenticationProvider;
import com.adventuretube.auth.service.CustomUserDetailService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static com.adventuretube.auth.config.security.SecurityConstants.OPEN_ENDPOINTS;


/**
 * Security filter chain for endpoints under /auth/**
 * - Applies only to /auth/** routes.
 * - Public access for login, registration, token refresh, logout, and Swagger-related paths.
 * - ADMIN role required for any other /auth/** endpoints.
 * - Integrates a custom JwtAuthFilter to process JWT tokens before default auth mechanisms.
 */
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class AuthServiceConfig {

    private  final CustomUserDetailService customUserDetailService;
    private  final JwtAuthFilter jwtAuthFilter;
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws  Exception{


        httpSecurity
            .csrf(AbstractHttpConfigurer::disable)
            .securityMatcher("/auth/**")// Applies this security configuration to /auth/** endpoints
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                       OPEN_ENDPOINTS // Public endpoints
                    )
                    .permitAll()
                    .anyRequest().hasRole("ADMIN")
            )
            //.authenticationProvider()
            /**
            // * Integrates a custom JwtAuthFilter to handle token-based authentication.
            // *
            // * - This filter is executed before Spring Security's default UsernamePasswordAuthenticationFilter.
            // * - It extracts the JWT from the Authorization header, validates its signature and expiration,
            // *   and sets the Authentication object in the SecurityContextHolder.
            // *
            // * - This Authentication object includes the user's roles, which are then used by Spring Security
            // *   to enforce access control based on the rules defined in the authorizeHttpRequests block above.
            // */
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
            //.httpBasic(withDefaults());// Use HTTP Basic authentication

     return httpSecurity.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /*
    This can be used to customize userDetailService
    This will allow to customize loadUserByUsername
       at this moment loadUSerByUser name does validate user email address and return  all error accordingly
     */
    @Bean
    public AuthenticationManager customAuthenticationManager(HttpSecurity httpSecurity) throws  Exception {
        AuthenticationManagerBuilder  authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(customAuthenticationProvider());
        //declare what type of authenticate provider will be used (userDetailService in our case)
        //and set the password encoder
        //authenticationManagerBuilder.userDetailsService(customUserDetailService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build(); // and return authenticationManager

    }


     /*This can be used for CustomAuthenticationProvider
      this can allow to  customize authenticate() method
     */

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider(customUserDetailService, passwordEncoder());
    }
}
