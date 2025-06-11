package com.adventuretube.auth.integration.controller;

import com.adventuretube.auth.integration.google.support.GoogleTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@AutoConfigureMockMvc
public class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;


    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${GOOGLE_REFRESH_TOKEN}")
    private String refreshToken;

    private String fetchGoogleIdToken() {
        return GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);
    }


//    @BeforeEach
//    void deleteTestUserIfExists() {
//        deleteUserViaMemberService("strider.lee@gmail.com");
//    }

    @Test
    void testRegisterUser() throws Exception {
        //deleteUserViaMemberService("strider.lee@gmail.com");

        String idToken = fetchGoogleIdToken();

        String body = String.format("""
                    {
                      "googleIdToken": "%s",
                      "email": "strider.lee@gmail.com",
                      "role": "USER",
                      "channelId": "UC_fake_channel"
                    }
                """, idToken);

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void testLoginAfterRegistration() throws Exception {
        registerTestUser("strider.lee@gmail.com"); // shared helper

        String idToken = fetchGoogleIdToken();
        String loginBody = String.format("""
        {
          "email": "strider.lee@gmail.com",
          "password": "123456",
          "googleIdToken": "%s"
        }
    """, idToken);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void testRefreshToken() throws Exception {
        deleteUserViaMemberService("strider.lee@gmail.com"); // Clean state
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

        String responseBody = mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = extractFieldFromJson(responseBody, "refreshToken");

        // 2. Refresh using valid token
        mockMvc.perform(post("/auth/token/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }


    @Test
    void testLogout() throws Exception {
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

        String responseBody = mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = extractFieldFromJson(responseBody, "accessToken");

        // Step 2: Logout using access token
        mockMvc.perform(post("/auth/token/revoke")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout has been successful"));

        // (Optional) Step 3: Cleanup user
        deleteUserViaMemberService("strider.lee@gmail.com");
    }


    private void deleteUserViaMemberService(String email) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(email, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity("http://MEMBER-SERVICE/member/deleteUser", entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Test user deleted.");
            } else {
                System.err.println("⚠️ Failed to delete test user: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("User cleanup skipped (probably not found): " + e.getMessage());
        }
    }

    private void registerTestUser(String email) throws Exception {
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

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());
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



