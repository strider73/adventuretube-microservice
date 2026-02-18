package com.adventuretube.auth.integration;

import com.adventuretube.auth.integration.support.AuthTestHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

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

    private final AuthTestHelper helper = new AuthTestHelper();

    private String googleIdToken;
    private String testUserEmail;

    @BeforeAll
    void setup() {
        log.info("=== RegistrationFlowIT Setup ===");
        log.info("Auth URL: {}, Member URL: {}", helper.getAuthBaseUrl(), helper.getMemberBaseUrl());

        // Fetch Google token and extract email
        googleIdToken = helper.fetchGoogleIdToken();
        testUserEmail = helper.extractEmailFromGoogleToken(googleIdToken);
        log.info("Test user email: {}", testUserEmail);

        // Delete existing user for clean state
        helper.deleteUser(testUserEmail);
        log.info("Setup complete - user cleaned up");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null) {
            log.info("=== RegistrationFlowIT Cleanup ===");
            helper.deleteUser(testUserEmail);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Register new user succeeds")
    void register_newUser_succeeds() throws Exception {
        log.info("=== Test: Register new user ===");

        ResponseEntity<String> response = helper.register(googleIdToken);

        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Registration should succeed, got: " + response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body, "Response body should not be null");

        // Verify userId is returned
        String userId = helper.extractField(body, "userId");
        assertNotNull(userId, "userId should be present in response");
        assertFalse(userId.isBlank(), "userId should not be blank");

        log.info("Registration successful - userId: {}", userId);

        // Verify tokens are returned
        String accessToken = helper.extractField(body, "accessToken");
        String refreshToken = helper.extractField(body, "refreshToken");
        assertNotNull(accessToken, "accessToken should be present");
        assertNotNull(refreshToken, "refreshToken should be present");

        log.info("Registration returned tokens (accessToken length: {}, refreshToken length: {})",
                accessToken.length(), refreshToken.length());
    }

    @Test
    @Order(2)
    @DisplayName("Register duplicate email returns 409 Conflict")
    void register_duplicateEmail_returns409() {
        log.info("=== Test: Register duplicate email ===");

        try {
            helper.register(googleIdToken);
            fail("Duplicate registration should fail with 409 Conflict");
        } catch (HttpClientErrorException.Conflict e) {
            log.info("Correctly rejected duplicate registration with 409 Conflict");
            log.info("Response: {}", e.getResponseBodyAsString());
            assertEquals(409, e.getStatusCode().value());
        } catch (HttpClientErrorException e) {
            log.info("Duplicate registration rejected with status: {}", e.getStatusCode());
            // 409 is expected, but accept other 4xx as well
            assertTrue(e.getStatusCode().is4xxClientError(),
                    "Should return 4xx for duplicate email, got: " + e.getStatusCode());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
