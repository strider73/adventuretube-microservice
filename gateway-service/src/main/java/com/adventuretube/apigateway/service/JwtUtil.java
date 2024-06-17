package com.adventuretube.apigateway.service;


import com.adventuretube.apigateway.exception.AccessDeniedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
        } catch (SignatureException | ExpiredJwtException e) { // Invalid signature or expired token
            log.error(e.getMessage());
            throw new AccessDeniedException("Access denied: " + e.getMessage());
        }

    }


    public void validateToken(String token) {
        getClaims(token); // Will throw an exception if invalid
    }


}
