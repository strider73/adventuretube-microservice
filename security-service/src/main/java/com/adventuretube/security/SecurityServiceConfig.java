package com.adventuretube.security;

import com.adventuretube.security.service.CustomUserDetailService;
import lombok.AllArgsConstructor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
//import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import static org.springframework.security.config.Customizer.withDefaults;
//
//import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityServiceConfig {

    private final CustomUserDetailService userDetailsService;

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws  Exception{
    httpSecurity
            .csrf().disable()
            .securityMatcher("/auth/**")
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/auth/register","/auth/getToken").permitAll()
                    .anyRequest().hasRole("ADMIN")
            )
//            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/register")) // Disable CSRF for /auth/register
            .httpBasic(withDefaults());

     return httpSecurity.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws  Exception {
        AuthenticationManagerBuilder  authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        //declare what type of authenticate provider will be used (userDetailService in our case)
        //and set the password encoder
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();

    }
//
//    @Bean
//    @Order(1)
//    public SecurityFilterChain formLoginFilterChain(HttpSecurity httpSecurity) throws  Exception{
//        httpSecurity
//                .authorizeHttpRequests(authorize -> authorize
//                        .anyRequest().authenticated()
//                )
//                .formLogin(withDefaults());
//
//        return httpSecurity.build();
//
//    }


}
