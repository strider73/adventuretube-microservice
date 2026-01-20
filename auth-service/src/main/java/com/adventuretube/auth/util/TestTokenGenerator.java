package com.adventuretube.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class to generate valid JWT tokens for testing purposes
 * This mimics the behavior of JwtUtil but allows for easy test data generation
 */
public class TestTokenGenerator {
    
    // Your actual JWT configuration values
    private static final String SECRET = "p1tX8G1LEA75ztxooQQ58iReDB6buWJ8wf9T+uWSyTk=";
    private static final long ACCESS_TOKEN_EXPIRATION = 120; // minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 86400; // minutes
    
    private static Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public static String generateAccessToken(String userId, String role) {
        return generateToken(userId, role, "ACCESS");
    }
    
    public static String generateRefreshToken(String userId, String role) {
        return generateToken(userId, role, "REFRESH");
    }
    
    private static String generateToken(String userId, String role, String tokenType) {
        Map<String, String> claims = Map.of("id", userId, "role", role);
        long expMillis = "ACCESS".equalsIgnoreCase(tokenType)
                ? ACCESS_TOKEN_EXPIRATION * 60 * 1000  // minutes to milliseconds
                : REFRESH_TOKEN_EXPIRATION * 60 * 1000; // minutes to milliseconds

        final Date now = new Date();
        final Date exp = new Date(now.getTime() + expMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey())
                .compact();
    }
    
    // Main method to generate test tokens for database insertion
    public static void main(String[] args) {
        System.out.println("-- Generated JWT tokens for test data");
        
        // Generate tokens for test users
        for (int i = 1; i <= 100; i++) {
            String userId = UUID.randomUUID().toString();
            String role = "USER";
            
            String accessToken = generateAccessToken(userId, role);
            String refreshToken = generateRefreshToken(userId, role);
            
            System.out.println("-- User " + i + " tokens:");
            System.out.println("-- Access Token: " + accessToken);
            System.out.println("-- Refresh Token: " + refreshToken);
            System.out.println();
        }
    }
}