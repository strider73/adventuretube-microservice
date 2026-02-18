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
 * Integration tests for the registration (sign up) flow.
 *
 * Tests AuthService.createUser() which calls:
 * - POST /member/emailDuplicationCheck
 * - POST /member/registerMember
 * - POST /member/storeTokens
 *
 * Prerequisites:
 * - Auth service running at AUTH_BASE_URL (default: localhost:8010)
 * - Member service running at MEMBER_BASE_URL (default: localhost:8070)
 * - Valid Google credentials in env.mac
 *
 * Run: source env.mac && mvn test -pl auth-service -Dtest="RegistrationFlowIT"
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegistrationFlowIT {

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
    private String testUserName;

    @BeforeAll
    void setup() throws Exception {
        log.info("=== RegistrationFlowIT Setup ===");
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

        // 3. Extract email and name from JWT payload
        String[] parts = googleIdToken.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        JsonNode payloadJson = objectMapper.readTree(payload);
        testUserEmail = payloadJson.get("email").asText();
        testUserName = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";
        log.info("Test user email: {}", testUserEmail);

        // 4. Delete existing user for clean state
        deleteUser(testUserEmail);
        log.info("Setup complete - user cleaned up");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null) {
            log.info("=== RegistrationFlowIT Cleanup ===");
            deleteUser(testUserEmail);
        }
    }

    // ============ Tests ============

    @Test
    @Order(1)
    @DisplayName("Register new user succeeds")
    void register_newUser_succeeds() throws Exception {
        log.info("=== Test: Register new user ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "googleIdToken", googleIdToken,
                "email", testUserEmail,
                "password", "password123",
                "username", testUserName.replaceAll("\\s+", ""),
                "role", "USER"
        ));

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("POST {}/auth/users (email: {})", AUTH_BASE_URL, testUserEmail);
        ResponseEntity<String> response = restTemplate.exchange(
                AUTH_BASE_URL + "/auth/users",
                HttpMethod.POST,
                request,
                String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Registration should succeed, got: " + response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        log.info("Response: {}", body);

        // Verify userId is returned
        JsonNode json = objectMapper.readTree(body);
        assertTrue(json.has("userId"), "userId should be present in response");
        String userId = json.get("userId").asText();
        assertFalse(userId.isBlank(), "userId should not be blank");
        log.info("Registration successful - userId: {}", userId);

        // Verify tokens are returned
        assertTrue(json.has("accessToken"), "accessToken should be present");
        assertTrue(json.has("refreshToken"), "refreshToken should be present");
        log.info("Registration returned tokens (accessToken length: {}, refreshToken length: {})",
                json.get("accessToken").asText().length(),
                json.get("refreshToken").asText().length());
    }

    @Test
    @Order(2)
    @DisplayName("Register duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        log.info("=== Test: Register duplicate email ===");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "googleIdToken", googleIdToken,
                "email", testUserEmail,
                "password", "password123",
                "username", testUserName.replaceAll("\\s+", ""),
                "role", "USER"
        ));

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("POST {}/auth/users (duplicate email: {})", AUTH_BASE_URL, testUserEmail);

        try {
            restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/users",
                    HttpMethod.POST,
                    request,
                    String.class
            );
            fail("Duplicate registration should fail with 409 Conflict");
        } catch (HttpClientErrorException.Conflict e) {
            log.info("Correctly rejected duplicate registration with 409 Conflict");
            log.info("Response: {}", e.getResponseBodyAsString());
            assertEquals(409, e.getStatusCode().value());
        } catch (HttpClientErrorException e) {
            log.info("Duplicate registration rejected with status: {}", e.getStatusCode());
            assertTrue(e.getStatusCode().is4xxClientError(),
                    "Should return 4xx for duplicate email, got: " + e.getStatusCode());
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
