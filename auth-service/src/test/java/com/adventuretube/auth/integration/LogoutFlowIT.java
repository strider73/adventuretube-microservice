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
 * Integration tests for the logout (token revocation) flow.
 *
 * Tests AuthService.revokeToken() which calls:
 * - POST /member/deleteAllToken
 *
 * Prerequisites:
 * - Auth service running at AUTH_BASE_URL (default: localhost:8010)
 * - Member service running at MEMBER_BASE_URL (default: localhost:8070)
 * - Valid Google credentials in env.mac
 *
 * Run: source env.mac && mvn test -pl auth-service -Dtest="LogoutFlowIT"
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LogoutFlowIT {

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

    @BeforeAll
    void setup() throws Exception {
        log.info("=== LogoutFlowIT Setup ===");
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

        // 4. Clean up, register, and login to ensure user exists with valid tokens
        deleteUser(testUserEmail);

        HttpHeaders regHeaders = new HttpHeaders();
        regHeaders.setContentType(MediaType.APPLICATION_JSON);
        String regBody = objectMapper.writeValueAsString(Map.of(
                "googleIdToken", googleIdToken,
                "email", testUserEmail,
                "password", "password123",
                "username", name.replaceAll("\\s+", ""),
                "role", "USER"
        ));
        HttpEntity<String> regRequest = new HttpEntity<>(regBody, regHeaders);
        restTemplate.exchange(AUTH_BASE_URL + "/auth/users", HttpMethod.POST, regRequest, String.class);

        log.info("Setup complete - user registered");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null) {
            log.info("=== LogoutFlowIT Cleanup ===");
            deleteUser(testUserEmail);
        }
    }

    // ============ Tests ============

    @Test
    @Order(1)
    @DisplayName("Logout with valid token succeeds")
    void logout_withValidToken_succeeds() throws Exception {
        log.info("=== Test: Logout with valid token ===");

        // Login to get fresh tokens
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, loginHeaders);

        log.info("POST {}/auth/token (login for fresh tokens)", AUTH_BASE_URL);
        ResponseEntity<String> loginResponse = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/token",
                HttpMethod.POST,
                loginRequest,
                String.class
        );
        assertTrue(loginResponse.getStatusCode().is2xxSuccessful(), "Login should succeed");

        JsonNode loginJson = objectMapper.readTree(loginResponse.getBody());
        String refreshToken = loginJson.get("refreshToken").asText();
        assertNotNull(refreshToken, "Refresh token required for logout");
        log.info("Login successful - got refreshToken (length: {})", refreshToken.length());

        // Revoke the refresh token
        HttpHeaders revokeHeaders = new HttpHeaders();
        revokeHeaders.set("Authorization", refreshToken);
        HttpEntity<String> revokeRequest = new HttpEntity<>(revokeHeaders);

        log.info("POST {}/auth/token/revoke", AUTH_BASE_URL);
        ResponseEntity<String> revokeResponse = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/token/revoke",
                HttpMethod.POST,
                revokeRequest,
                String.class
        );

        assertTrue(revokeResponse.getStatusCode().is2xxSuccessful(),
                "Logout should succeed, got: " + revokeResponse.getStatusCode());

        String body = revokeResponse.getBody();
        assertNotNull(body, "Response body should not be null");
        assertTrue(body.contains("Logout has been successful"),
                "Response should contain success message, got: " + body);

        log.info("Logout successful: {}", body);
    }

    @Test
    @Order(2)
    @DisplayName("Refresh after logout fails")
    void logout_thenRefresh_fails() throws Exception {
        log.info("=== Test: Refresh after logout ===");

        // 1. Login to get fresh tokens
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, loginHeaders);

        log.info("POST {}/auth/token (login for fresh tokens)", AUTH_BASE_URL);
        ResponseEntity<String> loginResponse = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/token",
                HttpMethod.POST,
                loginRequest,
                String.class
        );
        assertTrue(loginResponse.getStatusCode().is2xxSuccessful(), "Login should succeed");

        JsonNode loginJson = objectMapper.readTree(loginResponse.getBody());
        String tokenToRevoke = loginJson.get("refreshToken").asText();
        log.info("Login successful - got refreshToken to revoke");

        // 2. Revoke the token
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
        log.info("Token revoked");

        // 3. Attempt refresh with the revoked token — should fail
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.setContentType(MediaType.APPLICATION_JSON);
        refreshHeaders.set("Authorization", tokenToRevoke);
        HttpEntity<String> refreshRequest = new HttpEntity<>(refreshHeaders);

        log.info("POST {}/auth/token/refresh (with revoked token — should fail)", AUTH_BASE_URL);

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
