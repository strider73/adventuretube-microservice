package com.adventuretube.apigateway.service;

public class TokenSanitizer {
    public static String sanitize(String token) {
        return token == null ? null : token.replaceFirst("(?i)^\\s*Bearer[\\s]+", "").trim();
    }
}
