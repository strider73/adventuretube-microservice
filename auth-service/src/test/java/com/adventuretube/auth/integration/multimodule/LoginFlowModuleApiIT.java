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
 * Module API test for the complete login flow.
 * Tests the auth-service endpoints with real Google tokens.
 *
 * This is an EXTERNAL test that runs against a deployed/running auth-service.
 * It does NOT start a Spring context - it only makes HTTP calls.
 *
 * Flow:
 * 1. Fetch Google ID Token (using refresh token)
 * 2. Register new user (POST /auth/users)
 * 3. Login with Google ID Token (POST /auth/token)
 * 4. Refresh token (POST /auth/token/refresh)
 * 5. Logout (POST /auth/token/revoke)
 *
 * Prerequisites:
 * - Auth service must be running at AUTH_BASE_URL
 * - Valid Google credentials in env.mac (project root)
 *
 * Run: mvn verify -Dit.test=LoginFlowModuleApiIT -pl auth-service
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginFlowModuleApiIT {

    // Load from env.mac file
    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;

    // Test against local auth-service or remote
    // Override with env var: AUTH_BASE_URL=https://gateway.adventuretube.net
    private static final String AUTH_BASE_URL = System.getenv("AUTH_BASE_URL") != null
            ? System.getenv("AUTH_BASE_URL")
            : "http://localhost:8010";

    // Member service URL for user cleanup
    // Override with env var: MEMBER_BASE_URL=https://gateway.adventuretube.net
    private static final String MEMBER_BASE_URL = System.getenv("MEMBER_BASE_URL") != null
            ? System.getenv("MEMBER_BASE_URL")
            : "http://localhost:8070";

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Store test user email for cleanup
    private static String testUserEmail;

    // Store tokens between tests
    private static String googleIdToken;
    private static String accessToken;
    private static String adventuretubeRefreshToken;
    private static String userId;

    @BeforeAll
    static void loadEnvVariables() {
        log.info("=== Loading environment variables from env.mac ===");

        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");

        clientId = env.get("GOOGLE_CLIENT_ID");
        clientSecret = env.get("GOOGLE_CLIENT_SECRET");
        refreshToken = env.get("GOOGLE_REFRESH_TOKEN");

        assertNotNull(clientId, "GOOGLE_CLIENT_ID not found in env.mac");
        assertNotNull(clientSecret, "GOOGLE_CLIENT_SECRET not found in env.mac");
        assertNotNull(refreshToken, "GOOGLE_REFRESH_TOKEN not found in env.mac");

        log.info("Environment variables loaded successfully");
        log.info("Client ID: {}...", clientId.substring(0, Math.min(20, clientId.length())));
    }

    @AfterAll
    static void cleanup() {
        if (testUserEmail != null) {
            log.info("=== Cleanup: Deleting test user ===");
            deleteUserViaMemberService(testUserEmail);
        }
    }

    /**
     * Delete user via member-service REST API.
     * Called before registration to ensure clean state and after tests for cleanup.
     */
    private static void deleteUserViaMemberService(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    MEMBER_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Test user deleted: {}", email);
            } else {
                log.warn("Failed to delete test user: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.info("User cleanup skipped (probably not found): {}", e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: Fetch Google ID Token using refresh token")
    void step1_fetchGoogleIdToken() {
        log.info("=== Step 1: Fetching Google ID Token ===");
        log.info("Using clientId: {}", clientId);

        googleIdToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);

        assertNotNull(googleIdToken, "Google ID Token should not be null");
        assertFalse(googleIdToken.isBlank(), "Google ID Token should not be blank");

        log.info("Successfully fetched Google ID Token");
        log.info("Token length: {} characters", googleIdToken.length());
        log.info("Token preview: {}...", googleIdToken.substring(0, Math.min(50, googleIdToken.length())));
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Register new user with Google ID Token")
    void step2_registerUser() throws Exception {
        log.info("=== Step 2: Register new user ===");

        assertNotNull(googleIdToken, "Google ID Token must be fetched first (run Step 1)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Extract email from Google ID token payload (Base64 decode middle part)
        String[] parts = googleIdToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        String email = payloadJson.get("email").asText();
        String name = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";

        // Store email for cleanup and delete existing user first
        testUserEmail = email;
        log.info("Cleaning up existing user before registration...");
        deleteUserViaMemberService(email);

        log.info("Extracted email from token: {}", email);
        log.info("Extracted name from token: {}", name);

        // Build registration request
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "googleIdToken", googleIdToken,
            "email", email,
            "password", "password123",
            "username", name.replaceAll("\\s+", ""),
            "role", "USER"
        ));

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("Attempting registration at {}/auth/users", AUTH_BASE_URL);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/users",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Response Status: {}", response.getStatusCode());
            log.info("Response Body: {}", response.getBody());
            log.info("User registered successfully!");

        } catch (HttpClientErrorException.Conflict e) {
            // 409 Conflict - email already exists
            log.error("Registration failed - Email already exists!");
            log.error("Response: {}", e.getResponseBodyAsString());
            fail("Email duplication detected. Delete user from database first.");

        } catch (HttpClientErrorException e) {
            // Other 4xx errors
            log.error("Registration failed with status: {}", e.getStatusCode());
            log.error("Response: {}", e.getResponseBodyAsString());
            fail("Registration failed: " + e.getStatusCode());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Login with Google ID Token")
    void step3_login() throws Exception {
        log.info("=== Step 3: Login with Google ID Token ===");

        assertNotNull(googleIdToken, "Google ID Token must be fetched first (run Step 1)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("Attempting login at {}/auth/token", AUTH_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
            AUTH_BASE_URL + "/auth/token",
            HttpMethod.POST,
            request,
            String.class
        );

        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Body: {}", response.getBody());

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Login should succeed");

        JsonNode json = objectMapper.readTree(response.getBody());

        if (json.has("accessToken")) {
            accessToken = json.get("accessToken").asText();
            log.info("Access token received (length: {})", accessToken.length());
        }
        if (json.has("refreshToken")) {
            adventuretubeRefreshToken = json.get("refreshToken").asText();
            log.info("Refresh token received (length: {})", adventuretubeRefreshToken.length());
        }
        if (json.has("userId")) {
            userId = json.get("userId").asText();
            log.info("User ID: {}", userId);
        }

        log.info("Login successful!");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Test token refresh")
    void step4_refreshToken() throws Exception {
        log.info("=== Step 4: Refresh Token ===");

        assertNotNull(adventuretubeRefreshToken, "Refresh token must be obtained first (run Step 3)");
        assertNotNull(googleIdToken, "Google ID Token required");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", adventuretubeRefreshToken);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("Attempting token refresh at {}/auth/token/refresh", AUTH_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
            AUTH_BASE_URL + "/auth/token/refresh",
            HttpMethod.POST,
            request,
            String.class
        );

        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Body: {}", response.getBody());

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Token refresh should succeed");

        JsonNode json = objectMapper.readTree(response.getBody());
        if (json.has("accessToken")) {
            String newAccessToken = json.get("accessToken").asText();
            assertNotNull(newAccessToken);
            log.info("New access token received");
            accessToken = newAccessToken;
        }
        if (json.has("refreshToken")) {
            adventuretubeRefreshToken = json.get("refreshToken").asText();
            log.info("Refresh token received (length: {})", adventuretubeRefreshToken.length());
        }

        log.info("Token refresh successful!");
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Test logout")
    void step5_logout() {

        log.info("=== Step 5: Logout ===");

        assertNotNull(adventuretubeRefreshToken, "Refresh token required for logout (run Step 3)");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", adventuretubeRefreshToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("Attempting logout at {}/auth/token/revoke", AUTH_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
            AUTH_BASE_URL + "/auth/token/revoke",
            HttpMethod.POST,
            request,
            String.class
        );
        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Body: {}", response.getBody());

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Logout should succeed");
        log.info("Logout successful!");
    }

    // ============ Error Handling Tests ============

    @Test
    @DisplayName("Test: Invalid Google token returns proper error")
    void testInvalidGoogleToken() throws Exception {
        log.info("=== Test: Invalid Google Token ===");

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
            // If no exception, check the response indicates failure
            JsonNode json = objectMapper.readTree(response.getBody());
            assertFalse(json.path("success").asBoolean(true), "Should indicate failure");
            log.info("Correctly handled invalid token with error response");
        } catch (HttpClientErrorException e) {
            log.info("Correctly rejected invalid token with status: {}", e.getStatusCode());
            log.info("Error body: {}", e.getResponseBodyAsString());
        }
    }

    /*
     * ========================================================================
     * TEMPORARILY DISABLED - Tests /auth/token/refresh endpoint
     * ========================================================================
     *
     * WHY DISABLED:
     * - Tests the /auth/token/refresh endpoint which now requires JWT validation
     * - Same reasons as step4_refreshToken above
     *
     * WHEN TO RE-ENABLE:
     * - After JWT tokenType implementation is complete
     * - After tests are updated to go through Gateway
     *
     * TODO: Re-enable after JWT tokenType implementation is complete
     * ========================================================================
     */
    // @Test
    // @DisplayName("Test: Missing Authorization header returns proper error")
    // void testMissingAuthHeader() throws Exception {
    //     log.info("=== Test: Missing Authorization Header ===");
    //
    //     HttpHeaders headers = new HttpHeaders();
    //     headers.setContentType(MediaType.APPLICATION_JSON);
    //     // No Authorization header
    //
    //     String requestBody = "{\"googleIdToken\": \"some_token\"}";
    //
    //     HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    //
    //     try {
    //         ResponseEntity<String> response = restTemplate.exchange(
    //             AUTH_BASE_URL + "/auth/token/refresh",
    //             HttpMethod.POST,
    //             request,
    //             String.class
    //         );
    //         log.info("Response: {}", response.getBody());
    //         JsonNode json = objectMapper.readTree(response.getBody());
    //         assertFalse(json.path("success").asBoolean(true), "Should indicate failure");
    //         log.info("Correctly handled missing auth header");
    //     } catch (HttpClientErrorException e) {
    //         log.info("Correctly rejected missing auth header with status: {}", e.getStatusCode());
    //         log.info("Error body: {}", e.getResponseBodyAsString());
    //     }
    // }
}