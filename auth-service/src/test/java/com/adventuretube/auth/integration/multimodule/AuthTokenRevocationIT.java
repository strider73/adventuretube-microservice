package com.adventuretube.auth.integration.multimodule;

import com.adventuretube.auth.support.EnvFileLoader;
import com.adventuretube.auth.support.GoogleTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Auth Service Token Revocation Integration Tests.
 * Tests that the auth-service properly handles token revocation (logout).
 *
 * This is an EXTERNAL test that runs against a deployed gateway.
 * It does NOT start a Spring context - it only makes HTTP calls.
 *
 * Test scenarios:
 * 1. Revoked token should be rejected after logout → 401/403/404
 *
 * Prerequisites:
 * - Gateway service must be running at GATEWAY_BASE_URL
 * - Auth service must be registered with Eureka
 * - Valid Google credentials in env.mac (project root)
 *
 * Run: mvn verify -Dit.test=AuthTokenRevocationIT -pl auth-service
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthTokenRevocationIT {

    // Load from env.mac file
    private String clientId;
    private String clientSecret;
    private String refreshToken;

    // Test against gateway
    private static final String GATEWAY_BASE_URL = System.getenv("GATEWAY_BASE_URL") != null
            ? System.getenv("GATEWAY_BASE_URL")
            : "https://gateway.adventuretube.net";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Test data - populated in setup
    private String googleIdToken;
    private String testUserEmail;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeAll
    void setup() throws Exception {
        log.info("=== AuthTokenRevocationIT Setup ===");

        // Load environment variables
        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");
        clientId = env.get("GOOGLE_CLIENT_ID");
        clientSecret = env.get("GOOGLE_CLIENT_SECRET");
        refreshToken = env.get("GOOGLE_REFRESH_TOKEN");

        assertNotNull(clientId, "GOOGLE_CLIENT_ID not found in env.mac");
        assertNotNull(clientSecret, "GOOGLE_CLIENT_SECRET not found in env.mac");
        assertNotNull(refreshToken, "GOOGLE_REFRESH_TOKEN not found in env.mac");

        // Fetch Google ID token
        googleIdToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);
        assertNotNull(googleIdToken, "Failed to fetch Google ID token");

        // Extract email from token
        String[] parts = googleIdToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        testUserEmail = payloadJson.get("email").asText();

        log.info("Test user email: {}", testUserEmail);
        log.info("Gateway URL: {}", GATEWAY_BASE_URL);

        // Ensure user exists and get valid tokens
        ensureUserExistsAndLogin();

        log.info("Setup complete - valid tokens obtained");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null && validAccessToken != null) {
            log.info("=== Cleanup: Deleting test user ===");
            deleteUser(testUserEmail, validAccessToken);
        }
    }

    private void ensureUserExistsAndLogin() throws Exception {
        tryRegisterUser();
        login();
    }

    private void tryRegisterUser() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String[] parts = googleIdToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        String name = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "googleIdToken", googleIdToken,
                "email", testUserEmail,
                "password", "password123",
                "username", name.replaceAll("\\s+", ""),
                "role", "USER"
        ));

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(
                    GATEWAY_BASE_URL + "/auth/users",
                    HttpMethod.POST,
                    request,
                    String.class
            );
            log.info("User registered for token revocation tests");
        } catch (HttpClientErrorException.Conflict e) {
            log.info("User already exists - continuing with tests");
        }
    }

    private void login() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                GATEWAY_BASE_URL + "/auth/token",
                HttpMethod.POST,
                request,
                String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Login should succeed");

        JsonNode json = objectMapper.readTree(response.getBody());
        validAccessToken = json.get("accessToken").asText();
        validRefreshToken = json.get("refreshToken").asText();

        assertNotNull(validAccessToken, "Access token should be present");
        assertNotNull(validRefreshToken, "Refresh token should be present");

        log.info("Login successful - tokens obtained");
    }

    private boolean deleteUser(String email, String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + jwtToken);

        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Delete user failed: {}", e.getMessage());
            return false;
        }
    }

    // ============ Token Revocation Test ============

    @Test
    @DisplayName("Revoked token should be rejected after logout")
    void revokedToken_afterLogout_returns401() throws Exception {
        log.info("=== Test: Revoked token after logout ===");

        // Get fresh tokens for this test
        login();
        String tokenToRevoke = validRefreshToken;

        // Logout (revoke the refresh token)
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.set("Authorization", "Bearer " + tokenToRevoke);

        HttpEntity<String> logoutRequest = new HttpEntity<>(logoutHeaders);

        ResponseEntity<String> logoutResponse = restTemplate.exchange(
                GATEWAY_BASE_URL + "/auth/token/revoke",
                HttpMethod.POST,
                logoutRequest,
                String.class
        );

        assertTrue(logoutResponse.getStatusCode().is2xxSuccessful(), "Logout should succeed");
        log.info("Logout successful - token revoked");

        // Try to use the revoked refresh token for token refresh
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.setContentType(MediaType.APPLICATION_JSON);
        refreshHeaders.set("Authorization", "Bearer " + tokenToRevoke);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
        HttpEntity<String> refreshRequest = new HttpEntity<>(requestBody, refreshHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GATEWAY_BASE_URL + "/auth/token/refresh",
                    HttpMethod.POST,
                    refreshRequest,
                    String.class
            );
            log.warn("Token refresh returned: {} - {}", response.getStatusCode(), response.getBody());
            // If it doesn't throw, check if the response indicates failure
            JsonNode json = objectMapper.readTree(response.getBody());
            if (json.has("success") && !json.get("success").asBoolean()) {
                log.info("Token refresh correctly rejected revoked token");
            } else {
                fail("Revoked token should be rejected, but got: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            log.info("Received expected error for revoked token: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            // 401, 403, or 404 (TOKEN_NOT_FOUND) are acceptable for revoked tokens
            assertTrue(e.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                            e.getStatusCode() == HttpStatus.FORBIDDEN ||
                            e.getStatusCode() == HttpStatus.NOT_FOUND,
                    "Should return 401, 403, or 404 for revoked token, got: " + e.getStatusCode());
        }

        // Re-login to get valid tokens for cleanup
        login();
    }
}
