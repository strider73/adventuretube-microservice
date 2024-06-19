package com.adventuretube.security;

import com.adventuretube.security.filter.JwtAuthFilter;
import com.adventuretube.security.provider.CustomAuthenticationProvider;
import com.adventuretube.security.service.CustomUserDetailService;
import lombok.AllArgsConstructor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

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
                    .requestMatchers("/auth/register","/auth/getToken").permitAll()
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
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws  Exception {
        AuthenticationManagerBuilder  authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        //declare what type of authenticate provider will be used (userDetailService in our case)
        //and set the password encoder
//        authenticationManagerBuilder.authenticationProvider(customAuthenticationProvider());
//        return authenticationManagerBuilder.build();
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build(); // and return authenticationManager

    }

//    @Bean
//    public CustomAuthenticationProvider customAuthenticationProvider() {
//        CustomAuthenticationProvider customAuthenticationProvider = new CustomAuthenticationProvider();
//        customAuthenticationProvider.setUserDetailsService(userDetailsService);
//        customAuthenticationProvider.setPasswordEncoder(passwordEncoder());
//        return customAuthenticationProvider;
//    }


}
