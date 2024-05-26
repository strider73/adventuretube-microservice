package com.adventuretube;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public UserDetailsService userDetailsService(){

         InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
         manager.createUser(User
                 .withDefaultPasswordEncoder()
                 .username("strider")
                 .password("5785ch")
                 .roles("ADMIN")
                 .build());
        return manager;
/*
        UserDetails admin = User.withUsername("strider")
                .password(encoder.encode("5785ch"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.withUsername("chris")
                .password(encoder.encode("5785ch"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(admin,user);
*/
    }


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

//    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity.csrf().disable()
//                .authorizeHttpRequests()
//                .requestMatchers("/products/welcome").permitAll()
//                .requestMatchers("/products/**").authenticated()
//                .
//    }
}
