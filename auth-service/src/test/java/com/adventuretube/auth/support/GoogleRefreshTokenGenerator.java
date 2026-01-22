package com.adventuretube.auth.support;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class to generate Google OAuth refresh tokens for testing.
 *
 * This class automates the OAuth 2.0 authorization flow:
 * 1. Starts a local HTTP server to receive the callback
 * 2. Opens the browser for user authorization
 * 3. Captures the authorization code from the callback
 * 4. Exchanges the code for access and refresh tokens
 *
 * Usage:
 *   Run the main method with your Google OAuth credentials as arguments:
 *   java GoogleRefreshTokenGenerator <client_id> <client_secret>
 *
 *   Or use environment variables:
 *   GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET
 */
public class GoogleRefreshTokenGenerator {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final int CALLBACK_PORT = 8085;
    private static final String REDIRECT_URI = "http://localhost:" + CALLBACK_PORT + "/callback";
    private static final String SCOPES = "openid email profile";

    public static void main(String[] args) {
        String clientId;
        String clientSecret;

        if (args.length >= 2) {
            clientId = args[0];
            clientSecret = args[1];
        } else {
            // Try environment variables
            clientId = System.getenv("GOOGLE_CLIENT_ID");
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        }

        if (clientId == null || clientSecret == null) {
            System.err.println("Usage: java GoogleRefreshTokenGenerator <client_id> <client_secret>");
            System.err.println("Or set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables");
            System.exit(1);
        }

        try {
            String refreshToken = generateRefreshToken(clientId, clientSecret);
            System.out.println("\n" + "=".repeat(60));
            System.out.println("SUCCESS! Copy this refresh token to your env.mac file:");
            System.out.println("=".repeat(60));
            System.out.println("\nGOOGLE_REFRESH_TOKEN=" + refreshToken);
            System.out.println("\n" + "=".repeat(60));
        } catch (Exception e) {
            System.err.println("Failed to generate refresh token: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Generates a new Google refresh token through the OAuth 2.0 flow.
     * Opens a browser for user authorization and captures the tokens.
     */
    public static String generateRefreshToken(String clientId, String clientSecret) throws Exception {
        AtomicReference<String> authCode = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Start local server to receive callback
        HttpServer server = startCallbackServer(authCode, latch);

        try {
            // Build authorization URL
            String authUrl = buildAuthUrl(clientId);

            System.out.println("Opening browser for Google authorization...");
            System.out.println("If browser doesn't open, visit this URL manually:");
            System.out.println(authUrl);

            // Open browser
            openBrowser(authUrl);

            // Wait for callback (timeout after 2 minutes)
            System.out.println("\nWaiting for authorization (timeout: 2 minutes)...");
            if (!latch.await(2, TimeUnit.MINUTES)) {
                throw new RuntimeException("Authorization timeout - no callback received");
            }

            String code = authCode.get();
            if (code == null || code.isEmpty()) {
                throw new RuntimeException("No authorization code received");
            }

            System.out.println("Authorization code received, exchanging for tokens...");

            // Exchange code for tokens
            return exchangeCodeForRefreshToken(clientId, clientSecret, code);

        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startCallbackServer(AtomicReference<String> authCode, CountDownLatch latch) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = null;
            String error = null;

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if ("code".equals(pair[0])) {
                            code = pair[1];
                        } else if ("error".equals(pair[0])) {
                            error = pair[1];
                        }
                    }
                }
            }

            String response;
            if (error != null) {
                response = "<html><body><h1>Authorization Failed</h1><p>Error: " + error + "</p></body></html>";
            } else if (code != null) {
                authCode.set(code);
                response = "<html><body><h1>Authorization Successful!</h1><p>You can close this window and return to the terminal.</p></body></html>";
            } else {
                response = "<html><body><h1>Error</h1><p>No authorization code received</p></body></html>";
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            latch.countDown();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Callback server started on port " + CALLBACK_PORT);
        return server;
    }

    private static String buildAuthUrl(String clientId) {
        return AUTH_URL + "?" +
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(SCOPES, StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=consent";  // Force consent to always get refresh token
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Try OS-specific commands
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", url});
                } else if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
                } else {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
            }
        } catch (Exception e) {
            System.err.println("Could not open browser automatically: " + e.getMessage());
        }
    }

    private static String exchangeCodeForRefreshToken(String clientId, String clientSecret, String code) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", REDIRECT_URI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject json = new JSONObject(response.getBody());

                if (!json.has("refresh_token")) {
                    throw new RuntimeException("No refresh token in response. " +
                            "Make sure to use 'access_type=offline' and 'prompt=consent' in auth URL");
                }

                // Print all tokens for reference
                System.out.println("\nTokens received:");
                System.out.println("  Access Token: " + json.getString("access_token").substring(0, 20) + "...");
                if (json.has("id_token")) {
                    System.out.println("  ID Token: " + json.getString("id_token").substring(0, 20) + "...");
                }
                System.out.println("  Expires In: " + json.getInt("expires_in") + " seconds");

                return json.getString("refresh_token");
            } else {
                throw new RuntimeException("Token exchange failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error exchanging code for tokens: " + e.getMessage(), e);
        }
    }
}
