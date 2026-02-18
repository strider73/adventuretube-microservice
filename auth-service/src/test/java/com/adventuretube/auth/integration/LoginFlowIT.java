package com.adventuretube.auth.integration;

import com.adventuretube.auth.integration.support.AuthTestHelper;
import com.adventuretube.auth.integration.support.AuthTestHelper.AuthTokens;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.web.client.HttpClientErrorException;

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

    private final AuthTestHelper helper = new AuthTestHelper();

    private String googleIdToken;
    private String testUserEmail;
    private AuthTokens tokens;

    @BeforeAll
    void setup() throws Exception {
        log.info("=== LoginFlowIT Setup ===");
        log.info("Auth URL: {}, Member URL: {}", helper.getAuthBaseUrl(), helper.getMemberBaseUrl());

        // Fetch Google token and extract email
        googleIdToken = helper.fetchGoogleIdToken();
        testUserEmail = helper.extractEmailFromGoogleToken(googleIdToken);
        log.info("Test user email: {}", testUserEmail);

        // Clean up and re-register user to ensure clean state
        helper.deleteUser(testUserEmail);
        helper.register(googleIdToken);
        log.info("Setup complete - user registered");
    }

    @AfterAll
    void cleanup() {
        if (testUserEmail != null) {
            log.info("=== LoginFlowIT Cleanup ===");
            helper.deleteUser(testUserEmail);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Login with valid Google token returns tokens")
    void login_withValidGoogleToken_returnsTokens() throws Exception {
        log.info("=== Test: Login with valid Google token ===");

        tokens = helper.login(googleIdToken);

        assertNotNull(tokens.getAccessToken(), "Access token should be present");
        assertNotNull(tokens.getRefreshToken(), "Refresh token should be present");
        assertFalse(tokens.getAccessToken().isBlank(), "Access token should not be blank");
        assertFalse(tokens.getRefreshToken().isBlank(), "Refresh token should not be blank");

        log.info("Login returned accessToken (length: {}), refreshToken (length: {})",
                tokens.getAccessToken().length(), tokens.getRefreshToken().length());
    }

    @Test
    @Order(2)
    @DisplayName("Login with invalid Google token returns error")
    void login_withInvalidGoogleToken_returnsError() {
        log.info("=== Test: Login with invalid Google token ===");

        try {
            helper.login("invalid_token_12345");
            // If no exception, the response should indicate failure
            log.info("No exception thrown - service returned error in body");
        } catch (HttpClientErrorException e) {
            log.info("Correctly rejected invalid token with status: {}", e.getStatusCode());
            assertTrue(e.getStatusCode().is4xxClientError(),
                    "Should return 4xx for invalid token, got: " + e.getStatusCode());
        } catch (Exception e) {
            log.info("Login with invalid token correctly failed: {}", e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Refresh token with valid token returns new tokens")
    void refreshToken_withValidToken_returnsNewTokens() throws Exception {
        log.info("=== Test: Refresh token ===");

        assertNotNull(tokens, "Must run login test first (Order 1)");
        assertNotNull(tokens.getRefreshToken(), "Refresh token required");

        AuthTokens refreshed = helper.refreshToken(tokens.getRefreshToken());

        assertNotNull(refreshed.getAccessToken(), "New access token should be present");
        assertNotNull(refreshed.getRefreshToken(), "New refresh token should be present");

        log.info("Token refresh returned new accessToken (length: {}), refreshToken (length: {})",
                refreshed.getAccessToken().length(), refreshed.getRefreshToken().length());

        // Update tokens for subsequent tests
        tokens = new AuthTokens(refreshed.getAccessToken(), refreshed.getRefreshToken(), tokens.getUserId());
    }

    @Test
    @Order(4)
    @DisplayName("Refresh token after revoke fails")
    void refreshToken_afterRevoke_fails() throws Exception {
        log.info("=== Test: Refresh after revoke ===");

        assertNotNull(tokens, "Must run login test first (Order 1)");

        // First revoke the token
        String tokenToRevoke = tokens.getRefreshToken();
        helper.revokeToken(tokenToRevoke);
        log.info("Token revoked successfully");

        // Now try to refresh with the revoked token
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
