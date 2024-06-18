package com.adventuretube.apigateway.service;


import com.adventuretube.apigateway.exception.AccessDeniedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
    public void initKey(){
        this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
        //this.SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); // or HS384, or HS512


    }

   // this one already include expiration check
    public Claims getClaims(String token){
        try {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            // Log the exception or handle it based on your application's requirements
            log.error("JWT Token validation error :"+e.getMessage());
            throw  e;
        } catch (Exception e) {
            // Handle other unexpected exceptions
            throw new RuntimeException("Unexpected error while extracting claims from JWT token: " + e.getMessage(), e);
        }

    }




}
