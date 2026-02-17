package com.adventuretube.auth.unit.security;

import com.adventuretube.auth.filter.JwtWebFilter;
import com.adventuretube.auth.service.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtWebFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ReactiveUserDetailsService reactiveUserDetailsService;

    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    void setUp() {
        jwtWebFilter = new JwtWebFilter(jwtUtil, reactiveUserDetailsService);
    }

    @Test
    void filter_openEndpoint_shouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/users").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(reactiveUserDetailsService);
    }

    @Test
    void filter_actuatorHealth_shouldPassThroughSilently() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_protectedEndpointWithNoToken_shouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/something").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void filter_validJwt_shouldSetAuthentication() {
        String token = "valid-jwt-token";
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("encoded-password")
                .authorities("USER")
                .build();

        when(jwtUtil.extractUsername(token)).thenReturn("test@example.com");
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);
        when(reactiveUserDetailsService.findByUsername("test@example.com")).thenReturn(Mono.just(userDetails));

        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/something")
                .header(HttpHeaders.AUTHORIZATION, token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Use a chain that verifies security context is set
        WebFilterChain chain = filterExchange ->
                ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .doOnNext(auth -> {
                            assert auth != null;
                            assert auth.getName().equals("test@example.com");
                        })
                        .then();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtUtil).extractUsername(token);
        verify(jwtUtil).validateToken(token, userDetails);
        verify(reactiveUserDetailsService).findByUsername("test@example.com");
    }

    @Test
    void filter_invalidJwt_shouldPassThroughWithoutAuth() {
        String token = "invalid-jwt-token";
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("encoded-password")
                .authorities("USER")
                .build();

        when(jwtUtil.extractUsername(token)).thenReturn("test@example.com");
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);
        when(reactiveUserDetailsService.findByUsername("test@example.com")).thenReturn(Mono.just(userDetails));

        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/something")
                .header(HttpHeaders.AUTHORIZATION, token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void filter_jwtExtractionThrowsException_shouldPassThrough() {
        String token = "malformed-token";

        when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("Malformed token"));

        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/something")
                .header(HttpHeaders.AUTHORIZATION, token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void filter_nullUsername_shouldPassThrough() {
        String token = "some-token";

        when(jwtUtil.extractUsername(token)).thenReturn(null);

        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/admin/something")
                .header(HttpHeaders.AUTHORIZATION, token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(reactiveUserDetailsService);
    }

    @Test
    void filter_swaggerEndpoint_shouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/swagger-ui/index.html").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        WebFilterChain chain = filterExchange -> Mono.empty();

        StepVerifier.create(jwtWebFilter.filter(exchange, chain))
                .verifyComplete();

        verifyNoInteractions(jwtUtil);
    }
}
