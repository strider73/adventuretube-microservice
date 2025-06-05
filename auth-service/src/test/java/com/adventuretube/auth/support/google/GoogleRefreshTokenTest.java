package com.adventuretube.auth.support.google;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class GoogleRefreshTokenTest {

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${GOOGLE_REFRESH_TOKEN}")
    private String refreshToken;

    @Test
    void testGoogleIdTokenUsingRefreshToken() {
        String idToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);

        assertNotNull(idToken, "ID Token should not be null");
        assertFalse(idToken.isBlank(), "ID Token should not be blank");

        log.info("Successfully fetched Google ID Token: {}", idToken);
    }
}
