package com.adventuretube.auth.integration.multimodule;

import com.adventuretube.auth.support.GoogleTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot integration test for the complete login flow.
 * Tests the auth-service endpoints using WebTestClient (reactive, in-memory, no network).
 *
 * This is an INTERNAL test that starts a Spring Boot context.
 * Uses WebTestClient for testing - the reactive equivalent of MockMvc.
 *
 * Flow:
 * 1. Register new user (POST /auth/users)
 * 2. Login with Google ID Token (POST /auth/token)
 * 3. Refresh token (POST /auth/token/refresh)
 * 4. Logout (POST /auth/token/revoke)
 *
 * Prerequisites:
 * - Valid Google credentials configured in application-integration.yml
 * - Member service available for user cleanup
 *
 * Run: mvn verify -Dit.test=LoginFlowSpringBootIT -pl auth-service
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@AutoConfigureWebTestClient
public class LoginFlowSpringBootIT {

    @Autowired
    private WebTestClient webTestClient;

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${GOOGLE_REFRESH_TOKEN}")
    private String refreshToken;

    private String fetchGoogleIdToken() {
        return GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);
    }

    @Test
    void testRegisterUser() {
        String idToken = fetchGoogleIdToken();

        String body = String.format("""
                    {
                      "googleIdToken": "%s",
                      "email": "strider.lee@gmail.com",
                      "role": "USER",
                      "channelId": "UC_fake_channel"
                    }
                """, idToken);

        webTestClient.post().uri("/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.userId").exists();
    }

    @Test
    void testLoginAfterRegistration() {
        registerTestUser("strider.lee@gmail.com");

        String idToken = fetchGoogleIdToken();
        String loginBody = String.format("""
        {
          "email": "strider.lee@gmail.com",
          "password": "123456",
          "googleIdToken": "%s"
        }
    """, idToken);

        webTestClient.post().uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists();
    }

    @Test
    void testRefreshToken() {
        deleteUserViaMemberService("strider.lee@gmail.com");
        String idToken = fetchGoogleIdToken();

        // 1. Register the user and capture tokens
        String registerBody = String.format("""
        {
          "googleIdToken": "%s",
          "email": "strider.lee@gmail.com",
          "role": "USER",
          "channelId": "UC_fake_channel"
        }
    """, idToken);

        String responseBody = webTestClient.post().uri("/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        String refreshToken = extractFieldFromJson(responseBody, "refreshToken");

        // 2. Refresh using valid token
        webTestClient.post().uri("/auth/token/refresh")
                .header("Authorization", "Bearer " + refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists();
    }


    @Test
    void testLogout() {
        deleteUserViaMemberService("strider.lee@gmail.com");
        String idToken = fetchGoogleIdToken();

        // Step 1: Register user
        String registerBody = String.format("""
        {
          "googleIdToken": "%s",
          "email": "strider.lee@gmail.com",
          "role": "USER",
          "channelId": "UC_fake_channel"
        }
    """, idToken);

        String responseBody = webTestClient.post().uri("/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        String accessToken = extractFieldFromJson(responseBody, "accessToken");

        // Step 2: Logout using access token
        webTestClient.post().uri("/auth/token/revoke")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Logout has been successful");

        // (Optional) Step 3: Cleanup user
        deleteUserViaMemberService("strider.lee@gmail.com");
    }


    private void deleteUserViaMemberService(String email) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity("http://MEMBER-SERVICE/member/deleteUser", entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Test user deleted.");
            } else {
                System.err.println("Failed to delete test user: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("User cleanup skipped (probably not found): " + e.getMessage());
        }
    }

    private void registerTestUser(String email) {
        deleteUserViaMemberService(email);
        String idToken = fetchGoogleIdToken();

        String registerBody = String.format("""
        {
          "googleIdToken": "%s",
          "refreshToken": "dummy-refresh-token",
          "email": "%s",
          "password": "123456",
          "username": "testuser",
          "role": "USER",
          "channelId": "UC_fake_channel"
        }
    """, idToken, email);

        webTestClient.post().uri("/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerBody)
                .exchange()
                .expectStatus().isCreated();
    }

    private String extractFieldFromJson(String json, String fieldName) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(json)
                    .get(fieldName)
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
