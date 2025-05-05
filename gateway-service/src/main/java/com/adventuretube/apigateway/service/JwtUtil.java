package com.adventuretube.apigateway.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key SECRET_KEY;

    @PostConstruct
    public void initKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims getClaims(String token) {
        try {
            // Strip "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.error("JWT Token validation error :"+e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while extracting claims: " + e.getMessage(), e);
        }
    }

}

