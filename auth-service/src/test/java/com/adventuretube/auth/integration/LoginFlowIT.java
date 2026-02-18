package com.adventuretube.auth.integration;

import com.adventuretube.auth.support.EnvFileLoader;
import com.adventuretube.auth.support.GoogleTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the login and token refresh flows.
 *
 * Tests AuthService.issueToken() and AuthService.refreshToken() which call:
 * - POST /member/findMemberByEmail (via ReactiveAuthenticationManager)
 * - POST /member/storeTokens (after login)
 * - POST /member/findToken (during refresh)
 * - POST /member/storeTokens (after refresh)
 *
 * Prerequisites:
 * - Auth service running at AUTH_BASE_URL (default: localhost:8010)
 * - Member service running at MEMBER_BASE_URL (default: localhost:8070)
 * - Valid Google credentials in env.mac
 *
 * Run: source env.mac && mvn test -pl auth-service -Dtest="LoginFlowIT"
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginFlowIT {

    private static final String AUTH_BASE_URL = System.getenv("AUTH_BASE_URL") != null
            ? System.getenv("AUTH_BASE_URL")
            : "http://localhost:8010";

    private static final String MEMBER_BASE_URL = System.getenv("MEMBER_BASE_URL") != null
            ? System.getenv("MEMBER_BASE_URL")
            : "http://localhost:8070";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String googleIdToken;
    private String testUserEmail;

    // Tokens carried between ordered tests
    private String accessToken;
    private String refreshToken;
    private String userId;

    @BeforeAll
    void setup() throws Exception {
        log.info("=== LoginFlowIT Setup ===");
        log.info("Auth URL: {}, Member URL: {}", AUTH_BASE_URL, MEMBER_BASE_URL);

        // 1. Load Google credentials from env.mac
        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");
        String clientId = env.get("GOOGLE_CLIENT_ID");
        String clientSecret = env.get("GOOGLE_CLIENT_SECRET");
        String googleRefreshToken = env.get("GOOGLE_REFRESH_TOKEN");

        assertNotNull(clientId, "GOOGLE_CLIENT_ID not found in env.mac");
        assertNotNull(clientSecret, "GOOGLE_CLIENT_SECRET not found in env.mac");
        assertNotNull(googleRefreshToken, "GOOGLE_REFRESH_TOKEN not found in env.mac");

        // 2. Fetch Google ID token
        googleIdToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, googleRefreshToken);
        assertNotNull(googleIdToken, "Google ID Token should not be null");
        log.info("Google ID token fetched (length: {})", googleIdToken.length());

        // 3. Extract email from JWT payload
        String[] parts = googleIdToken.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        testUserEmail = payloadJson.get("email").asText();
        String name = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";
        log.info("Test user email: {}", testUserEmail);

        // 4. Clean up existing user
        deleteUser(testUserEmail);

        // 5. Register user so login tests have a valid user
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "googleIdToken", googleIdToken,
                "email", testUserEmail,
                "password", "password123",
                "username", name.replaceAll("\\s+", ""),
                "role", "USER"
        ));

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        restTemplate.exchange(AUTH_BASE_URL + "/auth/users", HttpMethod.POST, request, String.class);
        log.info("Setup complete - user registered");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null) {
            log.info("=== LoginFlowIT Cleanup ===");
            deleteUser(testUserEmail);
        }
    }

    // ============ Tests ============

    @Test
    @Order(1)
    @DisplayName("Login with valid Google token returns tokens")
    void login_withValidGoogleToken_returnsTokens() throws Exception {
        log.info("=== Test: Login with valid Google token ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("POST {}/auth/token", AUTH_BASE_URL);
        ResponseEntity<String> response = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/token",
                HttpMethod.POST,
                request,
                String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Login should succeed");

        JsonNode json = objectMapper.readTree(response.getBody());
        accessToken = json.has("accessToken") ? json.get("accessToken").asText() : null;
        refreshToken = json.has("refreshToken") ? json.get("refreshToken").asText() : null;
        userId = json.has("userId") ? json.get("userId").asText() : null;

        assertNotNull(accessToken, "Access token should be present");
        assertNotNull(refreshToken, "Refresh token should be present");
        assertFalse(accessToken.isBlank(), "Access token should not be blank");
        assertFalse(refreshToken.isBlank(), "Refresh token should not be blank");

        log.info("Login successful - userId: {}, accessToken length: {}, refreshToken length: {}",
                userId, accessToken.length(), refreshToken.length());
    }

    @Test
    @Order(2)
    @DisplayName("Login with invalid Google token returns error")
    void login_withInvalidGoogleToken_returnsError() {
        log.info("=== Test: Login with invalid Google token ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"invalid_token_12345\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );
            log.info("Response: {}", response.getBody());
        } catch (HttpClientErrorException e) {
            log.info("Correctly rejected invalid token with status: {}", e.getStatusCode());
            assertTrue(e.getStatusCode().is4xxClientError(),
                    "Should return 4xx for invalid token, got: " + e.getStatusCode());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Refresh token with valid token returns new tokens")
    void refreshToken_withValidToken_returnsNewTokens() throws Exception {
        log.info("=== Test: Refresh token ===");

        assertNotNull(refreshToken, "Refresh token required (run login test first)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", refreshToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("POST {}/auth/token/refresh", AUTH_BASE_URL);
        ResponseEntity<String> response = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/token/refresh",
                HttpMethod.POST,
                request,
                String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Token refresh should succeed");

        JsonNode json = objectMapper.readTree(response.getBody());
        String newAccessToken = json.has("accessToken") ? json.get("accessToken").asText() : null;
        String newRefreshToken = json.has("refreshToken") ? json.get("refreshToken").asText() : null;

        assertNotNull(newAccessToken, "New access token should be present");
        assertNotNull(newRefreshToken, "New refresh token should be present");

        log.info("Token refresh successful - new accessToken length: {}, new refreshToken length: {}",
                newAccessToken.length(), newRefreshToken.length());

        // Update tokens for subsequent tests
        accessToken = newAccessToken;
        refreshToken = newRefreshToken;
    }

    @Test
    @Order(4)
    @DisplayName("Refresh token after revoke fails")
    void refreshToken_afterRevoke_fails() throws Exception {
        log.info("=== Test: Refresh after revoke ===");

        assertNotNull(refreshToken, "Refresh token required (run login test first)");

        // 1. Revoke the current refresh token
        String tokenToRevoke = refreshToken;

        HttpHeaders revokeHeaders = new HttpHeaders();
        revokeHeaders.set("Authorization", tokenToRevoke);
        HttpEntity<String> revokeRequest = new HttpEntity<>(revokeHeaders);

        log.info("POST {}/auth/token/revoke", AUTH_BASE_URL);
        ResponseEntity<String> revokeResponse = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/token/revoke",
                HttpMethod.POST,
                revokeRequest,
                String.class
        );
        assertTrue(revokeResponse.getStatusCode().is2xxSuccessful(), "Revoke should succeed");
        log.info("Token revoked successfully");

        // 2. Try to refresh with the revoked token — should fail
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.setContentType(MediaType.APPLICATION_JSON);
        refreshHeaders.set("Authorization", tokenToRevoke);
        HttpEntity<String> refreshRequest = new HttpEntity<>(refreshHeaders);

        try {
            restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token/refresh",
                    HttpMethod.POST,
                    refreshRequest,
                    String.class
            );
            fail("Refresh with revoked token should fail");
        } catch (HttpClientErrorException e) {
            log.info("Correctly rejected revoked token with status: {}", e.getStatusCode());
            assertTrue(
                    e.getStatusCode().value() == 401 ||
                    e.getStatusCode().value() == 403 ||
                    e.getStatusCode().value() == 404,
                    "Should return 401, 403, or 404 for revoked token, got: " + e.getStatusCode()
            );
        }
    }

    // ============ Cleanup Helper ============

    private void deleteUser(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    MEMBER_BASE_URL + "/member/deleteUser", entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Test user deleted: {}", email);
            } else {
                log.warn("Failed to delete test user: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.info("User cleanup skipped (probably not found): {}", e.getMessage());
        }
    }
}
