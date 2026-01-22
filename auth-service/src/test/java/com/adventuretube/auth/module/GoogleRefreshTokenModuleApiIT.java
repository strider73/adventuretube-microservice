package com.adventuretube.auth.module;

import com.adventuretube.auth.support.EnvFileLoader;
import com.adventuretube.auth.support.GoogleTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module API test for Google refresh token functionality.
 * Tests fetching Google ID token using refresh token.
 *
 * This is an EXTERNAL test - no Spring context needed.
 * Loads credentials from env.mac file.
 *
 * Run: mvn verify -Dit.test=GoogleRefreshTokenModuleApiIT -pl auth-service
 */
@Slf4j
public class GoogleRefreshTokenModuleApiIT {

    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;

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
    }

    @Test
    void testGoogleIdTokenUsingRefreshToken() {
        log.info("=== Testing Google ID Token fetch ===");

        String idToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);

        assertNotNull(idToken, "ID Token should not be null");
        assertFalse(idToken.isBlank(), "ID Token should not be blank");

        log.info("Successfully fetched Google ID Token");
        log.info("Token length: {} characters", idToken.length());
        log.info("Token preview: {}...", idToken.substring(0, Math.min(50, idToken.length())));
    }
}
