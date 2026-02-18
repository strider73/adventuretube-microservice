package com.adventuretube.auth.integration;

import com.adventuretube.auth.integration.support.AuthTestHelper;
import com.adventuretube.auth.integration.support.AuthTestHelper.AuthTokens;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

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

    private final AuthTestHelper helper = new AuthTestHelper();

    private String googleIdToken;
    private String testUserEmail;
    private AuthTokens tokens;

    @BeforeAll
    void setup() throws Exception {
        log.info("=== LogoutFlowIT Setup ===");
        log.info("Auth URL: {}, Member URL: {}", helper.getAuthBaseUrl(), helper.getMemberBaseUrl());

        // Fetch Google token and extract email
        googleIdToken = helper.fetchGoogleIdToken();
        testUserEmail = helper.extractEmailFromGoogleToken(googleIdToken);
        log.info("Test user email: {}", testUserEmail);

        // Ensure user exists: clean up, register, then login
        helper.deleteUser(testUserEmail);
        helper.register(googleIdToken);
        tokens = helper.login(googleIdToken);

        assertNotNull(tokens.getAccessToken(), "Setup: access token required");
        assertNotNull(tokens.getRefreshToken(), "Setup: refresh token required");
        log.info("Setup complete - user registered and logged in");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null) {
            log.info("=== LogoutFlowIT Cleanup ===");
            helper.deleteUser(testUserEmail);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Logout with valid token succeeds")
    void logout_withValidToken_succeeds() throws Exception {
        log.info("=== Test: Logout with valid token ===");

        // Login fresh to get a valid token for this test
        tokens = helper.login(googleIdToken);
        assertNotNull(tokens.getRefreshToken(), "Refresh token required for logout");

        ResponseEntity<String> response = helper.revokeToken(tokens.getRefreshToken());

        assertTrue(response.getStatusCode().is2xxSuccessful(),
                "Logout should succeed, got: " + response.getStatusCode());

        String body = response.getBody();
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

        // Login fresh, then revoke
        tokens = helper.login(googleIdToken);
        String tokenToRevoke = tokens.getRefreshToken();

        ResponseEntity<String> revokeResponse = helper.revokeToken(tokenToRevoke);
        assertTrue(revokeResponse.getStatusCode().is2xxSuccessful(), "Revoke should succeed");
        log.info("Token revoked");

        // Attempt refresh with the revoked token
        try {
            helper.refreshToken(tokenToRevoke);
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
}
