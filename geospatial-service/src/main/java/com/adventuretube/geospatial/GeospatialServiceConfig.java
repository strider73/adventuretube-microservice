package com.adventuretube.geospatial;

import com.adventuretube.geospatial.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@RequiredArgsConstructor
public class GeospatialServiceConfig {

//    @Bean
//    public RestTemplate restTemplate(){
//          return new RestTemplate();
//    }
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
            http.csrf().disable()//RestAPI with JWT doen't require csrf protection
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/security/**").hasRole("ADMIN")  // Restrict /security/** to ADMIN role
                        .anyRequest().permitAll()  // Allow all other requests
                )
//                .csrf(csrf -> csrf.ignoringRequestMatchers("/member/**")) // Disable CSRF for /auth/register
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Add your custom JWT filter
                    .httpBasic(withDefaults());
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
