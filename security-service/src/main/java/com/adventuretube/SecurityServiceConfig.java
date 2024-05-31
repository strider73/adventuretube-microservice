package com.adventuretube;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityServiceConfig {


    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws  Exception{
    httpSecurity
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> authorize
                    .anyRequest().hasRole("ADMIN")
            )
            .httpBasic(withDefaults());

     return httpSecurity.build();

    }

    @Bean
    @Order(1)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity httpSecurity) throws  Exception{
        httpSecurity
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .formLogin(withDefaults());

        return httpSecurity.build();

    }

    @Bean
    // after create eureka server without @LoadBalanced  annotation  restTemplate call
    // will get the error of unknown host !!!!
    //end if you have more than one instance to call the request will be loadbalanced
    //and able to check from the log
    @LoadBalanced
    public RestTemplate restTemplate(){
        return  new RestTemplate();
    }
}
