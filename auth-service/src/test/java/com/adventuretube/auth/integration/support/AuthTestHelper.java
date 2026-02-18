package com.adventuretube.auth.integration.support;

import com.adventuretube.auth.support.EnvFileLoader;
import com.adventuretube.auth.support.GoogleTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Shared helper for auth-service integration tests.
 * Provides common HTTP operations and response parsing used across
 * LoginFlowIT, RegistrationFlowIT, and LogoutFlowIT.
 *
 * Uses plain RestTemplate (no Spring context required).
 * Target URLs default to localhost and can be overridden via env vars.
 */
@Slf4j
public class AuthTestHelper {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String authBaseUrl;
    private final String memberBaseUrl;

    public AuthTestHelper() {
        this.authBaseUrl = System.getenv("AUTH_BASE_URL") != null
                ? System.getenv("AUTH_BASE_URL")
                : "http://localhost:8010";
        this.memberBaseUrl = System.getenv("MEMBER_BASE_URL") != null
                ? System.getenv("MEMBER_BASE_URL")
                : "http://localhost:8070";
    }

    public String getAuthBaseUrl() {
        return authBaseUrl;
    }

    public String getMemberBaseUrl() {
        return memberBaseUrl;
    }

    // ============ Google Token ============

    /**
     * Loads Google credentials from env.mac and fetches a fresh Google ID token.
     */
    public String fetchGoogleIdToken() {
        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");

        String clientId = env.get("GOOGLE_CLIENT_ID");
        String clientSecret = env.get("GOOGLE_CLIENT_SECRET");
        String refreshToken = env.get("GOOGLE_REFRESH_TOKEN");

        assertNotNull(clientId, "GOOGLE_CLIENT_ID not found in env.mac");
        assertNotNull(clientSecret, "GOOGLE_CLIENT_SECRET not found in env.mac");
        assertNotNull(refreshToken, "GOOGLE_REFRESH_TOKEN not found in env.mac");

        log.info("Fetching Google ID token with clientId: {}...",
                clientId.substring(0, Math.min(20, clientId.length())));

        String idToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);
        assertNotNull(idToken, "Google ID Token should not be null");

        log.info("Google ID token fetched (length: {})", idToken.length());
        return idToken;
    }

    /**
     * Extracts email from a Google ID token by Base64-decoding the JWT payload.
     */
    public String extractEmailFromGoogleToken(String googleIdToken) {
        try {
            String[] parts = googleIdToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payloadJson = objectMapper.readTree(payload);
            return payloadJson.get("email").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract email from Google token", e);
        }
    }

    /**
     * Extracts the "name" field from a Google ID token payload.
     */
    public String extractNameFromGoogleToken(String googleIdToken) {
        try {
            String[] parts = googleIdToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payloadJson = objectMapper.readTree(payload);
            return payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";
        } catch (Exception e) {
            return "TestUser";
        }
    }

    // ============ Auth Operations ============

    /**
     * POST /auth/users — Register a new user.
     * Returns the raw response entity (caller decides on assertions).
     */
    public ResponseEntity<String> register(String googleIdToken) throws Exception {
        String email = extractEmailFromGoogleToken(googleIdToken);
        String name = extractNameFromGoogleToken(googleIdToken);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "googleIdToken", googleIdToken,
                "email", email,
                "password", "password123",
                "username", name.replaceAll("\\s+", ""),
                "role", "USER"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("POST {}/auth/users (email: {})", authBaseUrl, email);
        return restTemplate.exchange(
                authBaseUrl + "/auth/users",
                HttpMethod.POST,
                request,
                String.class
        );
    }

    /**
     * POST /auth/token — Login with Google ID token.
     * Returns AuthTokens (accessToken + refreshToken).
     */
    public AuthTokens login(String googleIdToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        log.info("POST {}/auth/token", authBaseUrl);
        ResponseEntity<String> response = restTemplate.exchange(
                authBaseUrl + "/auth/token",
                HttpMethod.POST,
                request,
                String.class
        );

        JsonNode json = objectMapper.readTree(response.getBody());
        String accessToken = json.has("accessToken") ? json.get("accessToken").asText() : null;
        String refreshToken = json.has("refreshToken") ? json.get("refreshToken").asText() : null;
        String userId = json.has("userId") ? json.get("userId").asText() : null;

        log.info("Login successful (userId: {})", userId);
        return new AuthTokens(accessToken, refreshToken, userId);
    }

    /**
     * POST /auth/token/refresh — Refresh tokens using a refresh token.
     * Returns AuthTokens with new accessToken + refreshToken.
     */
    public AuthTokens refreshToken(String refreshToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", refreshToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("POST {}/auth/token/refresh", authBaseUrl);
        ResponseEntity<String> response = restTemplate.exchange(
                authBaseUrl + "/auth/token/refresh",
                HttpMethod.POST,
                request,
                String.class
        );

        JsonNode json = objectMapper.readTree(response.getBody());
        String accessToken = json.has("accessToken") ? json.get("accessToken").asText() : null;
        String newRefreshToken = json.has("refreshToken") ? json.get("refreshToken").asText() : null;

        log.info("Token refresh successful");
        return new AuthTokens(accessToken, newRefreshToken, null);
    }

    /**
     * POST /auth/token/revoke — Logout / revoke token.
     * Returns the raw response entity.
     */
    public ResponseEntity<String> revokeToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);

        HttpEntity<String> request = new HttpEntity<>(headers);

        log.info("POST {}/auth/token/revoke", authBaseUrl);
        return restTemplate.exchange(
                authBaseUrl + "/auth/token/revoke",
                HttpMethod.POST,
                request,
                String.class
        );
    }

    // ============ Cleanup ============

    /**
     * POST /member/deleteUser — Delete a user directly via member-service.
     * Used for test cleanup. Silently ignores failures (user may not exist).
     */
    public void deleteUser(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    memberBaseUrl + "/member/deleteUser",
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

    // ============ JSON Utilities ============

    /**
     * Extracts a string field from a JSON response body.
     */
    public String extractField(String json, String field) {
        try {
            return objectMapper.readTree(json).get(field).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract field '" + field + "' from JSON", e);
        }
    }

    // ============ Token Holder ============

    /**
     * Holds access and refresh tokens returned from auth operations.
     */
    public static class AuthTokens {
        private final String accessToken;
        private final String refreshToken;
        private final String userId;

        public AuthTokens(String accessToken, String refreshToken, String userId) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userId = userId;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getUserId() { return userId; }
    }
}
