package com.adventuretube.security;

import com.adventuretube.security.filter.JwtAuthFilter;
import com.adventuretube.security.service.CustomUserDetailService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;
//
//import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityServiceConfig {

    private  final CustomUserDetailService userDetailsService;
    private  final JwtAuthFilter jwtAuthFilter;
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws  Exception{
    httpSecurity
            .csrf().disable()
            .securityMatcher("/auth/**")
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/auth/register","/auth/login","/auth/refreshToken").permitAll()
                    .anyRequest().hasRole("ADMIN")
            )
//            .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/register")) // Disable CSRF for /auth/register
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(withDefaults());

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
