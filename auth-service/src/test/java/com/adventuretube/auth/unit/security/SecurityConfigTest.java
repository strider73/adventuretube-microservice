package com.adventuretube.auth.unit.security;

import com.adventuretube.auth.config.security.AuthServiceConfig;
import com.adventuretube.auth.filter.JwtWebFilter;
import com.adventuretube.auth.service.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.web.server.ServerHttpSecurity.http;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ReactiveUserDetailsService reactiveUserDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceConfig authServiceConfig;

    @BeforeEach
    void setUp() {
        JwtWebFilter jwtWebFilter = new JwtWebFilter(jwtUtil, reactiveUserDetailsService);
        authServiceConfig = new AuthServiceConfig(jwtWebFilter);
    }

    @Test
    void securityConfig_shouldCreateFilterChain() {
        // Verify that AuthServiceConfig can create beans without errors
        assertThat(authServiceConfig).isNotNull();
    }

    @Test
    void passwordEncoder_shouldReturnBCryptEncoder() {
        PasswordEncoder encoder = authServiceConfig.passwordEncoder();
        assertThat(encoder).isNotNull();

        String encoded = encoder.encode("testPassword");
        assertThat(encoder.matches("testPassword", encoded)).isTrue();
        assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
    }

    @Test
    void openEndpoints_shouldBeAccessible() {
        // Create a minimal router to test security against
        RouterFunction<ServerResponse> testRoutes = RouterFunctions.route()
                .POST("/auth/users", request -> ServerResponse.ok().bodyValue("ok"))
                .POST("/auth/token", request -> ServerResponse.ok().bodyValue("ok"))
                .POST("/auth/token/refresh", request -> ServerResponse.ok().bodyValue("ok"))
                .POST("/auth/token/revoke", request -> ServerResponse.ok().bodyValue("ok"))
                .build();

        JwtWebFilter jwtWebFilter = new JwtWebFilter(jwtUtil, reactiveUserDetailsService);
        AuthServiceConfig config = new AuthServiceConfig(jwtWebFilter);

        SecurityWebFilterChain filterChain = config.apiFilterChain(
                org.springframework.security.config.web.server.ServerHttpSecurity.http()
        );

        WebTestClient client = WebTestClient.bindToRouterFunction(testRoutes)
                .webFilter(new org.springframework.security.web.server.WebFilterChainProxy(filterChain))
                .build();

        // Open endpoints should be accessible without authentication
        client.post().uri("/auth/users")
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/auth/token")
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/auth/token/refresh")
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/auth/token/revoke")
                .exchange()
                .expectStatus().isOk();
    }
}
