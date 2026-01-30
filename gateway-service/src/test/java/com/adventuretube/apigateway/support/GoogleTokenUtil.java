package com.adventuretube.apigateway.support;

import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Utility class to fetch Google ID tokens using a refresh token.
 * Used for Module API tests that need real Google authentication.
 */
public class GoogleTokenUtil {

    public static String fetchIdToken(String clientId, String clientSecret, String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject json = new JSONObject(response.getBody());
                return json.getString("id_token");
            } else {
                throw new RuntimeException("Failed to fetch ID token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving ID token from Google: " + e.getMessage(), e);
        }
    }
}
