package com.adventuretube.auth.config.security;


public class SecurityConstants {
    public static final String[] OPEN_ENDPOINTS = {
            "/auth/users",
            "/auth/token/**",
            "/auth/refreshToken",
            "/auth/logout",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };
}
