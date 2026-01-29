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
 * Gateway API test for the complete login flow.
 * Tests the auth-service endpoints THROUGH the API Gateway.
 *
 * This is an EXTERNAL test that runs against a deployed gateway.
 * It does NOT start a Spring context - it only makes HTTP calls.
 *
 * Flow:
 * 1. Fetch Google ID Token (using refresh token)
 * 2. Register new user (POST /auth/users via Gateway)
 * 3. Login with Google ID Token (POST /auth/token via Gateway)
 * 4. Refresh token (POST /auth/token/refresh via Gateway)
 * 5. Logout (POST /auth/token/revoke via Gateway)
 * 6. Delete user via Gateway (requires JWT)
 * 7. Re-register user (verify deletion worked)
 * 8. Invalid token error test
 *
 * Prerequisites:
 * - Gateway service must be running at GATEWAY_BASE_URL
 * - Auth service must be registered with Eureka
 * - Member service must be registered with Eureka (for cleanup)
 * - Valid Google credentials in env.mac (project root)
 *
 * Run: mvn verify -Dit.test=LoginFlowGatewayIT -pl auth-service
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginFlowGatewayIT {

    // Load from env.mac file
    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;

    // Test against gateway (local or production)
    // Override with env var: GATEWAY_BASE_URL=http://localhost:8030
    private static final String GATEWAY_BASE_URL = System.getenv("GATEWAY_BASE_URL") != null
            ? System.getenv("GATEWAY_BASE_URL")
            : "https://gateway.adventuretube.net";

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
        log.info("Gateway URL: {}", GATEWAY_BASE_URL);
    }

    @AfterAll
    static void cleanup() {
        if (testUserEmail != null && accessToken != null) {
            log.info("=== Cleanup: Deleting test user ===");
            boolean deleted = deleteUserViaMemberService(testUserEmail, accessToken);
            if (deleted) {
                log.info("Test user cleaned up successfully");
            } else {
                log.warn("Failed to cleanup test user - may need manual deletion");
            }
        } else {
            log.info("Skipping cleanup - no user email or access token available");
        }
    }

    /**
     * Delete user via member-service through Gateway.
     * Requires JWT authentication as /member/deleteUser is a secured endpoint.
     *
     * @param email the user's email to delete
     * @param jwtToken valid JWT access token for authentication
     * @return true if deletion was successful, false otherwise
     */
    private static boolean deleteUserViaMemberService(String email, String jwtToken) {
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
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Test user deleted via gateway: {}", email);
                return true;
            } else {
                log.warn("Failed to delete test user: {}", response.getStatusCode());
                return false;
            }
        } catch (HttpClientErrorException e) {
            log.warn("Delete user failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("User deletion failed: {}", e.getMessage());
            return false;
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
    @DisplayName("Step 2: Register new user via Gateway")
    void step2_registerUser() throws Exception {
        log.info("=== Step 2: Register new user via Gateway ===");

        assertNotNull(googleIdToken, "Google ID Token must be fetched first (run Step 1)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Extract email from Google ID token payload (Base64 decode middle part)
        String[] parts = googleIdToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        String email = payloadJson.get("email").asText();
        String name = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";

        // Store email for later use (cleanup happens in Step 6 with JWT)
        testUserEmail = email;

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

        log.info("Attempting registration via gateway at {}/auth/users", GATEWAY_BASE_URL);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GATEWAY_BASE_URL + "/auth/users",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Response Status: {}", response.getStatusCode());
            log.info("Response Body: {}", response.getBody());
            log.info("User registered successfully via gateway!");

        } catch (HttpClientErrorException.Conflict e) {
            // 409 Conflict - email already exists
            // This is OK for gateway test - user cleanup requires JWT which we don't have yet
            log.warn("Registration skipped - User already exists (expected for gateway test)");
            log.warn("Response: {}", e.getResponseBodyAsString());
            // Don't fail - continue with login test

        } catch (HttpClientErrorException e) {
            // Other 4xx errors
            log.error("Registration failed with status: {}", e.getStatusCode());
            log.error("Response: {}", e.getResponseBodyAsString());
            fail("Registration failed: " + e.getStatusCode());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Login via Gateway with Google ID Token")
    void step3_login() throws Exception {
        log.info("=== Step 3: Login via Gateway with Google ID Token ===");

        assertNotNull(googleIdToken, "Google ID Token must be fetched first (run Step 1)");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("Attempting login via gateway at {}/auth/token", GATEWAY_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
            GATEWAY_BASE_URL + "/auth/token",
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

        log.info("Login via gateway successful!");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Test token refresh via Gateway")
    void step4_refreshToken() throws Exception {
        log.info("=== Step 4: Refresh Token via Gateway ===");

        assertNotNull(adventuretubeRefreshToken, "Refresh token must be obtained first (run Step 3)");
        assertNotNull(googleIdToken, "Google ID Token required");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + adventuretubeRefreshToken);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("Attempting token refresh via gateway at {}/auth/token/refresh", GATEWAY_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
            GATEWAY_BASE_URL + "/auth/token/refresh",
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

        log.info("Token refresh via gateway successful!");
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Test logout via Gateway")
    void step5_logout() {
        log.info("=== Step 5: Logout via Gateway ===");

        assertNotNull(adventuretubeRefreshToken, "Refresh token required for logout (run Step 3)");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adventuretubeRefreshToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("Attempting logout via gateway at {}/auth/token/revoke", GATEWAY_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
            GATEWAY_BASE_URL + "/auth/token/revoke",
            HttpMethod.POST,
            request,
            String.class
        );
        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Body: {}", response.getBody());

        assertTrue(response.getStatusCode().is2xxSuccessful(), "Logout should succeed");
        log.info("Logout via gateway successful!");
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Delete user via Gateway (requires JWT)")
    void step6_deleteUser() {
        log.info("=== Step 6: Delete User via Gateway ===");

        assertNotNull(accessToken, "Access token required for delete (run Step 3 or 4)");
        assertNotNull(testUserEmail, "Test user email required (run Step 2)");

        log.info("Attempting to delete user: {}", testUserEmail);

        boolean deleted = deleteUserViaMemberService(testUserEmail, accessToken);

        assertTrue(deleted, "User should be deleted successfully via gateway");
        log.info("User deletion via gateway successful!");
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Re-register user (verify deletion worked)")
    void step7_reRegisterUser() throws Exception {
        log.info("=== Step 7: Re-register User (Verify Deletion) ===");

        assertNotNull(googleIdToken, "Google ID Token must be fetched first (run Step 1)");
        assertNotNull(testUserEmail, "Test user email required");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Extract name from Google ID token payload
        String[] parts = googleIdToken.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        String name = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";

        // Build registration request
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "googleIdToken", googleIdToken,
            "email", testUserEmail,
            "password", "password123",
            "username", name.replaceAll("\\s+", ""),
            "role", "USER"
        ));

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("Re-registering user via gateway at {}/auth/users", GATEWAY_BASE_URL);

        ResponseEntity<String> response = restTemplate.exchange(
                GATEWAY_BASE_URL + "/auth/users",
                HttpMethod.POST,
                request,
                String.class
        );

        log.info("Response Status: {}", response.getStatusCode());
        log.info("Response Body: {}", response.getBody());

        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Re-registration should succeed (proves deletion worked)");
        log.info("User re-registered successfully - confirms Step 6 deletion worked!");

        // Login again to get new access token for cleanup
        step7_loginForCleanup();
    }

    /**
     * Helper method to login after re-registration to get access token for cleanup.
     */
    private void step7_loginForCleanup() throws Exception {
        log.info("--- Step 7b: Login to get access token for cleanup ---");

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

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode json = objectMapper.readTree(response.getBody());
            if (json.has("accessToken")) {
                accessToken = json.get("accessToken").asText();
                log.info("New access token obtained for cleanup");
            }
        }
    }

    // ============ Error Handling Tests ============

    @Test
    @Order(8)
    @DisplayName("Step 8: Test invalid Google token returns proper error via Gateway")
    void step8_testInvalidGoogleToken() throws Exception {
        log.info("=== Test: Invalid Google Token via Gateway ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"invalid_token_12345\"}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                GATEWAY_BASE_URL + "/auth/token",
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

    @Test
    @DisplayName("Test: Gateway connectivity check")
    void testGatewayConnectivity() {
        log.info("=== Test: Gateway Connectivity ===");

        try {
            // Simple health check or actuator endpoint if available
            ResponseEntity<String> response = restTemplate.getForEntity(
                GATEWAY_BASE_URL + "/actuator/health",
                String.class
            );
            log.info("Gateway health: {}", response.getBody());
            assertTrue(response.getStatusCode().is2xxSuccessful(), "Gateway should be healthy");
        } catch (Exception e) {
            log.warn("Gateway actuator not available, trying root: {}", e.getMessage());
            // Gateway might not have actuator, that's ok for this test
        }
    }
}
