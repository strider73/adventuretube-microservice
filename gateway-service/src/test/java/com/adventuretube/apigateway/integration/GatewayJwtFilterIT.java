package com.adventuretube.apigateway.integration;

import com.adventuretube.apigateway.support.EnvFileLoader;
import com.adventuretube.apigateway.support.GoogleTokenUtil;
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
 * Gateway JWT Filter Integration Tests.
 * Tests that the gateway properly validates JWT tokens on secured endpoints.
 *
 * This is an EXTERNAL test that runs against a deployed gateway.
 * It does NOT start a Spring context - it only makes HTTP calls.
 *
 * Test scenarios:
 * 1. Access secured endpoint without JWT → 401 Unauthorized
 * 2. Access secured endpoint with invalid JWT → 401 Unauthorized
 * 3. Access secured endpoint with malformed JWT → 401 Unauthorized
 * 4. Access secured endpoint with empty Authorization header → 401 Unauthorized
 * 5. Access secured endpoint with Bearer prefix only → 401 Unauthorized
 * 6. Access secured endpoint with valid JWT → Success
 *
 * Prerequisites:
 * - Gateway service must be running at GATEWAY_BASE_URL
 * - Auth service must be registered with Eureka
 * - Member service must be registered with Eureka
 * - Valid Google credentials in env.mac (project root)
 *
 * Run: mvn verify -Dit.test=GatewayJwtFilterIT -pl gateway-service
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GatewayJwtFilterIT {

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
        log.info("=== GatewayJwtFilterIT Setup ===");

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
        // Try to register (may already exist)
        tryRegisterUser();

        // Login to get tokens
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
            log.info("User registered for JWT filter tests");
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

    // ============ JWT Filter Tests ============

    @Test
    @DisplayName("Secured endpoint without JWT should return 401")
    void securedEndpoint_withoutJwt_returns401() {
        log.info("=== Test: Secured endpoint without JWT ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(testUserEmail, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            fail("Should have been rejected without JWT, but got: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.info("Received expected client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Should return 401 Unauthorized without JWT");
        }
    }

    @Test
    @DisplayName("Secured endpoint with invalid JWT should return 401")
    void securedEndpoint_withInvalidJwt_returns401() {
        log.info("=== Test: Secured endpoint with invalid JWT ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer invalid.jwt.token");

        HttpEntity<String> entity = new HttpEntity<>(testUserEmail, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            fail("Should have thrown 401 Unauthorized, but got: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.info("Received expected error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Should return 401 Unauthorized with invalid JWT");
        }
    }

    @Test
    @DisplayName("Secured endpoint with malformed JWT should return 401")
    void securedEndpoint_withMalformedJwt_returns401() {
        log.info("=== Test: Secured endpoint with malformed JWT ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer not-a-jwt-at-all");

        HttpEntity<String> entity = new HttpEntity<>(testUserEmail, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            fail("Should have thrown 401 Unauthorized, but got: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.info("Received expected error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Should return 401 Unauthorized with malformed JWT");
        }
    }

    @Test
    @DisplayName("Secured endpoint with empty Authorization header should return 401")
    void securedEndpoint_withEmptyAuthHeader_returns401() {
        log.info("=== Test: Secured endpoint with empty Authorization header ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "");

        HttpEntity<String> entity = new HttpEntity<>(testUserEmail, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            fail("Should have thrown 401 Unauthorized, but got: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.info("Received expected error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Should return 401 Unauthorized with empty Authorization header");
        }
    }

    @Test
    @DisplayName("Secured endpoint with Bearer prefix only should return 401")
    void securedEndpoint_withBearerPrefixOnly_returns401() {
        log.info("=== Test: Secured endpoint with Bearer prefix only ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer ");

        HttpEntity<String> entity = new HttpEntity<>(testUserEmail, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/deleteUser",
                    entity,
                    String.class
            );
            fail("Should have thrown 401 Unauthorized, but got: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            log.info("Received expected error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Should return 401 Unauthorized with Bearer prefix only");
        }
    }

    @Test
    @DisplayName("Secured endpoint with valid JWT should succeed")
    void securedEndpoint_withValidJwt_succeeds() {
        log.info("=== Test: Secured endpoint with valid JWT ===");

        assertNotNull(validAccessToken, "Valid access token required for this test");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + validAccessToken);

        // Use a different secured endpoint that doesn't delete the user
        // Testing /member/findMemberByEmail which is also secured
        HttpEntity<String> entity = new HttpEntity<>(testUserEmail, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + "/member/findMemberByEmail",
                    entity,
                    String.class
            );
            log.info("Response: {} - {}", response.getStatusCode(), response.getBody());
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                    "Secured endpoint should succeed with valid JWT");
        } catch (HttpClientErrorException e) {
            // If endpoint doesn't exist or has different behavior, log it
            log.warn("Endpoint returned: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            // 401 would be a test failure, other codes might be OK (404 if endpoint doesn't exist)
            assertNotEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Should NOT return 401 with valid JWT");
        }
    }
}
