package com.adventuretube.auth.component.service;

import com.adventuretube.auth.config.google.GoogleTokenCredentialProperties;
import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.auth.model.mapper.MemberMapper;
import com.adventuretube.auth.model.mapper.MemberMapperImpl;
import com.adventuretube.auth.model.request.MemberLoginRequest;
import com.adventuretube.auth.model.request.MemberRegisterRequest;
import com.adventuretube.auth.model.response.MemberRegisterResponse;
import com.adventuretube.auth.service.AuthService;
import com.adventuretube.auth.service.CustomUserDetailService;
import com.adventuretube.auth.service.JwtUtil;
import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceComponentTest {

    private MockWebServer mockWebServer;
    private AuthService authService;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Test constants
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_GOOGLE_SUB = "google-subject-123";
    private static final String TEST_USERNAME = "Test User";
    private static final String TEST_ROLE = "USER";
    private static final String TEST_JWT_SECRET = "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciB0ZXN0aW5nIHB1cnBvc2VzIG9ubHk=";
    private static final String TEST_ACCESS_EXPIRATION = "3600";
    private static final String TEST_REFRESH_EXPIRATION = "86400";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();

        // Create plain (non-load-balanced) WebClient.Builder
        WebClient.Builder webClientBuilder = WebClient.builder();

        // No-op circuit breaker that passes through all calls
        ReactiveCircuitBreakerFactory<Object, ConfigBuilder<Object>> circuitBreakerFactory =
                new ReactiveCircuitBreakerFactory<>() {
                    @Override
                    public ReactiveCircuitBreaker create(String id) {
                        return new ReactiveCircuitBreaker() {
                            @Override
                            public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
                                return toRun; // pass through, ignore fallback
                            }
                            @Override
                            public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
                                return toRun; // pass through, ignore fallback
                            }
                        };
                    }
                    @Override
                    protected ConfigBuilder<Object> configBuilder(String id) { return null; }
                    @Override
                    public void configureDefault(Function<String, Object> defaultConfiguration) {}
                };

        ServiceClient serviceClient = new ServiceClient(webClientBuilder, circuitBreakerFactory);

        // Create JwtUtil with test values via reflection
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", TEST_ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", TEST_REFRESH_EXPIRATION);
        jwtUtil.initKey();

        // Create supporting beans
        passwordEncoder = new BCryptPasswordEncoder();
        MemberMapper memberMapper = new MemberMapperImpl();

        // Create GoogleTokenCredentialProperties
        GoogleTokenCredentialProperties googleProps = new GoogleTokenCredentialProperties();
        googleProps.setClientId("test-client-id");
        googleProps.setClientSecret("test-client-secret");

        // Create CustomUserDetailService with memberServiceUrl pointing to MockWebServer
        CustomUserDetailService customUserDetailService = new CustomUserDetailService(serviceClient);
        ReflectionTestUtils.setField(customUserDetailService, "memberServiceUrl", baseUrl);

        // Create ReactiveAuthenticationManager (same pattern as AuthServiceConfig)
        ReactiveAuthenticationManager reactiveAuthenticationManager = authentication -> {
            String email = authentication.getName();
            String password = authentication.getCredentials().toString();

            return customUserDetailService.findByUsername(email)
                    .filter(userDetails -> passwordEncoder.matches(password, userDetails.getPassword()))
                    .map(userDetails -> (org.springframework.security.core.Authentication)
                            new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities()))
                    .switchIfEmpty(reactor.core.publisher.Mono.error(
                            new BadCredentialsException("Invalid username or password")));
        };

        // Create TestableAuthService that overrides Google token verification
        authService = new TestableAuthService(
                googleProps, serviceClient, jwtUtil, passwordEncoder,
                reactiveAuthenticationManager, memberMapper
        );
        ReflectionTestUtils.setField(authService, "memberServiceUrl", baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ===== createUser tests =====

    @Test
    @DisplayName("createUser: success - returns userId and tokens")
    void createUser_success_returnsTokens() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode(TEST_GOOGLE_SUB);

        // 1. emailDuplicationCheck → false (not duplicate)
        enqueueSuccess(false);

        // 2. registerMember → MemberDTO
        MemberDTO registeredMember = MemberDTO.builder()
                .id(userId)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .password(encodedPassword)
                .role(TEST_ROLE)
                .build();
        enqueueSuccess(registeredMember);

        // 3. storeTokens → true
        enqueueSuccess(true);

        MemberRegisterRequest request = new MemberRegisterRequest(
                "fake-google-token", null, TEST_EMAIL, "password123", TEST_USERNAME, TEST_ROLE, null
        );

        StepVerifier.create(authService.createUser(request))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getUserId()).isEqualTo(userId);
                    assertThat(response.getData().getAccessToken()).isNotBlank();
                    assertThat(response.getData().getRefreshToken()).isNotBlank();
                    // Verify tokens are valid JWTs
                    assertThat(jwtUtil.extractUsername(response.getData().getAccessToken())).isEqualTo(TEST_EMAIL);
                    assertThat(jwtUtil.extractUsername(response.getData().getRefreshToken())).isEqualTo(TEST_EMAIL);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createUser: duplicate email - throws DuplicateException")
    void createUser_duplicateEmail_throwsDuplicateException() throws JsonProcessingException {
        // emailDuplicationCheck → true (duplicate exists)
        enqueueSuccess(true);

        MemberRegisterRequest request = new MemberRegisterRequest(
                "fake-google-token", null, TEST_EMAIL, "password123", TEST_USERNAME, TEST_ROLE, null
        );

        StepVerifier.create(authService.createUser(request))
                .expectError(DuplicateException.class)
                .verify();
    }

    @Test
    @DisplayName("createUser: member-service returns 500 - throws MemberServiceException")
    void createUser_memberServiceDown_throwsMemberServiceException() throws JsonProcessingException {
        // emailDuplicationCheck → 500
        enqueueError(500, "SERVER_NOT_AVAILABLE", "Service unavailable");

        MemberRegisterRequest request = new MemberRegisterRequest(
                "fake-google-token", null, TEST_EMAIL, "password123", TEST_USERNAME, TEST_ROLE, null
        );

        StepVerifier.create(authService.createUser(request))
                .expectError(MemberServiceException.class)
                .verify();
    }

    @Test
    @DisplayName("createUser: token store fails - throws TokenSaveFailedException")
    void createUser_tokenStoreFails_throwsTokenSaveFailedException() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();
        String encodedPassword = passwordEncoder.encode(TEST_GOOGLE_SUB);

        // 1. emailDuplicationCheck → false
        enqueueSuccess(false);

        // 2. registerMember → MemberDTO
        MemberDTO registeredMember = MemberDTO.builder()
                .id(userId)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .password(encodedPassword)
                .role(TEST_ROLE)
                .build();
        enqueueSuccess(registeredMember);

        // 3. storeTokens → false (failure)
        enqueueSuccess(false);

        MemberRegisterRequest request = new MemberRegisterRequest(
                "fake-google-token", null, TEST_EMAIL, "password123", TEST_USERNAME, TEST_ROLE, null
        );

        StepVerifier.create(authService.createUser(request))
                .expectError(TokenSaveFailedException.class)
                .verify();
    }

    // ===== issueToken tests =====

    @Test
    @DisplayName("issueToken: success - returns tokens")
    void issueToken_success_returnsTokens() throws JsonProcessingException {
        String encodedGoogleSub = passwordEncoder.encode(TEST_GOOGLE_SUB);

        // 1. findMemberByEmail (called by CustomUserDetailService via ReactiveAuthenticationManager)
        MemberDTO foundMember = MemberDTO.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .password(encodedGoogleSub)
                .username(TEST_USERNAME)
                .role(TEST_ROLE)
                .build();
        enqueueSuccess(foundMember);

        // 2. storeTokens → true
        enqueueSuccess(true);

        MemberLoginRequest request = new MemberLoginRequest(TEST_EMAIL, "password123", "fake-google-token");

        StepVerifier.create(authService.issueToken(request))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getAccessToken()).isNotBlank();
                    assertThat(response.getData().getRefreshToken()).isNotBlank();
                    assertThat(response.getData().getUserId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("issueToken: user not found - throws UserNotFoundException")
    void issueToken_userNotFound_throwsUserNotFoundException() throws JsonProcessingException {
        // findMemberByEmail → 404 USER_NOT_FOUND
        enqueueError(404, "USER_NOT_FOUND", "User not found");

        MemberLoginRequest request = new MemberLoginRequest(TEST_EMAIL, "password123", "fake-google-token");

        StepVerifier.create(authService.issueToken(request))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    // ===== refreshToken tests =====

    @Test
    @DisplayName("refreshToken: success - returns new tokens")
    void refreshToken_success_returnsNewTokens() throws JsonProcessingException {
        // Generate a valid token to use as the "existing" token
        String existingToken = jwtUtil.generate(TEST_EMAIL, TEST_ROLE, "REFRESH");

        // 1. findToken → true (token exists in DB)
        enqueueSuccess(true);

        // 2. storeTokens → true
        enqueueSuccess(true);

        StepVerifier.create(authService.refreshToken("Bearer " + existingToken))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getAccessToken()).isNotBlank();
                    assertThat(response.getData().getRefreshToken()).isNotBlank();
                    // Verify new tokens are valid JWTs with correct claims
                    assertThat(jwtUtil.extractUsername(response.getData().getAccessToken())).isEqualTo(TEST_EMAIL);
                    assertThat(jwtUtil.extractUserRole(response.getData().getAccessToken())).isEqualTo(TEST_ROLE);
                    assertThat(jwtUtil.extractUsername(response.getData().getRefreshToken())).isEqualTo(TEST_EMAIL);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("refreshToken: token not found in DB - throws TokenNotFoundException")
    void refreshToken_tokenNotFound_throwsTokenNotFoundException() throws JsonProcessingException {
        String existingToken = jwtUtil.generate(TEST_EMAIL, TEST_ROLE, "REFRESH");

        // findToken → 404 TOKEN_NOT_FOUND
        enqueueError(404, "TOKEN_NOT_FOUND", "Token not found");

        StepVerifier.create(authService.refreshToken("Bearer " + existingToken))
                .expectError(TokenNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("refreshToken: token found but store fails - throws TokenSaveFailedException")
    void refreshToken_tokenStoreFails_throwsTokenSaveFailedException() throws JsonProcessingException {
        String existingToken = jwtUtil.generate(TEST_EMAIL, TEST_ROLE, "REFRESH");

        // 1. findToken → true
        enqueueSuccess(true);

        // 2. storeTokens → false (failure)
        enqueueSuccess(false);

        StepVerifier.create(authService.refreshToken("Bearer " + existingToken))
                .expectError(TokenSaveFailedException.class)
                .verify();
    }

    // ===== revokeToken tests =====

    @Test
    @DisplayName("revokeToken: success - returns logout response")
    void revokeToken_success_returnsLogoutResponse() throws JsonProcessingException {
        String token = jwtUtil.generate(TEST_EMAIL, TEST_ROLE, "ACCESS");

        // deleteAllToken → true
        enqueueSuccess(true);

        StepVerifier.create(authService.revokeToken("Bearer " + token))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).isEqualTo("Logout has been successful");
                    assertThat(response.getData()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("revokeToken: deletion fails - throws TokenDeletionException")
    void revokeToken_deletionFails_throwsTokenDeletionException() throws JsonProcessingException {
        String token = jwtUtil.generate(TEST_EMAIL, TEST_ROLE, "ACCESS");

        // deleteAllToken → 500 TOKEN_DELETION_FAILED
        enqueueError(500, "TOKEN_DELETION_FAILED", "Failed to delete token");

        StepVerifier.create(authService.revokeToken("Bearer " + token))
                .expectError(TokenDeletionException.class)
                .verify();
    }

    @Test
    @DisplayName("revokeToken: member-service unavailable - throws MemberServiceException")
    void revokeToken_memberServiceUnavailable_throwsMemberServiceException() throws IOException {
        String token = jwtUtil.generate(TEST_EMAIL, TEST_ROLE, "ACCESS");

        // Shutdown MockWebServer to simulate connection refused
        mockWebServer.shutdown();

        StepVerifier.create(authService.revokeToken("Bearer " + token))
                .expectError(MemberServiceException.class)
                .verify();
    }

    // ===== Helper methods =====

    private <T> void enqueueSuccess(T data) throws JsonProcessingException {
        ServiceResponse<T> response = ServiceResponse.<T>builder()
                .success(true)
                .message("OK")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(response))
                .addHeader("Content-Type", "application/json"));
    }

    private void enqueueError(int httpStatus, String errorCode, String message) throws JsonProcessingException {
        ServiceResponse<Object> response = ServiceResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(httpStatus)
                .setBody(objectMapper.writeValueAsString(response))
                .addHeader("Content-Type", "application/json"));
    }

    /**
     * Test subclass that overrides Google token verification to return a fake GoogleIdToken.
     * This avoids needing real Google credentials for component tests.
     */
    static class TestableAuthService extends AuthService {

        TestableAuthService(GoogleTokenCredentialProperties googleTokenCredentialProperties,
                            ServiceClient serviceClient,
                            JwtUtil jwtUtil,
                            PasswordEncoder passwordEncoder,
                            ReactiveAuthenticationManager reactiveAuthenticationManager,
                            MemberMapper memberMapper) {
            super(googleTokenCredentialProperties, serviceClient, jwtUtil,
                    passwordEncoder, reactiveAuthenticationManager, memberMapper);
        }

        @Override
        protected GoogleIdToken verifyGoogleIdToken(String googleIdToken) {
            // Return a fake GoogleIdToken with test email and subject
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(TEST_EMAIL);
            payload.setSubject(TEST_GOOGLE_SUB);
            payload.set("name", TEST_USERNAME);
            payload.setExpirationTimeSeconds(System.currentTimeMillis() / 1000 + 3600);
            payload.setIssuedAtTimeSeconds(System.currentTimeMillis() / 1000);

            return new GoogleIdToken(
                    new GoogleIdToken.Header(),
                    payload,
                    new byte[0],
                    new byte[0]
            );
        }
    }
}
