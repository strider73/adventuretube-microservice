package com.adventuretube.member;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class MemberServiceConfig {

//    @Bean
//    public RestTemplate restTemplate(){
//          return new RestTemplate();
//    }


    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
            http.csrf().disable()//RestAPI with JWT doen't require csrf protection
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/security/**").hasRole("ADMIN")  // Restrict /security/** to ADMIN role
                        .anyRequest().permitAll()  // Allow all other requests
                )
//                .csrf(csrf -> csrf.ignoringRequestMatchers("/member/**")) // Disable CSRF for /auth/register
                .httpBasic(withDefaults());
        return http.build();
    }





}
