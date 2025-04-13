package com.adventuretube.auth;

import com.adventuretube.auth.filter.JwtAuthFilter;
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

    private  final CustomUserDetailService userDetailsService;
    private  final JwtAuthFilter jwtAuthFilter;
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws  Exception{
    httpSecurity
            .csrf(AbstractHttpConfigurer::disable)
            .securityMatcher("/auth/**")// Applies this security configuration to /auth/** endpoints
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                            "/auth/users",
                            "/auth/refreshToken",
                            "/auth/logout",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**"
                    )
                    .permitAll()
                    .anyRequest().hasRole("ADMIN")
            )
            //.authenticationProvider()
            /**
             * Integrates a custom JwtAuthFilter to handle token-based authentication.
             *
             * - This filter is executed before the default UsernamePasswordAuthenticationFilter.
             * - It extracts the JWT from the Authorization header, validates it,
             *   and populates the SecurityContextHolder with the authenticated user's details.
             *   in the JwtAuthFilter class
             *
             * - The authenticated user's roles are then used for access control
             *   based on the rules defined in the authorizeHttpRequests block above.
             */
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
        //declare what type of authenticate provider will be used (userDetailService in our case)
        //and set the password encoder
//        authenticationManagerBuilder.authenticationProvider(customAuthenticationProvider());
//        return authenticationManagerBuilder.build();
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build(); // and return authenticationManager

    }


     /*This can be used for CustomAuthenticationProvider
      this can allow to  customize authencate() method
     */

//    @Bean
//    public CustomAuthenticationProvider customAuthenticationProvider() {
//        CustomAuthenticationProvider customAuthenticationProvider = new CustomAuthenticationProvider();
//        customAuthenticationProvider.setUserDetailsService(userDetailsService);
//        customAuthenticationProvider.setPasswordEncoder(passwordEncoder());
//        return customAuthenticationProvider;
//    }


}
