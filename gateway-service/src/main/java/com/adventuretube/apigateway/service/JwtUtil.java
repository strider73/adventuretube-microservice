package com.adventuretube.apigateway.service;


import com.adventuretube.apigateway.exception.AccessDeniedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key SECRET_KEY;

    @PostConstruct
    public void initKey(){
        //this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
        this.SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); // or HS384, or HS512


    }


    public Claims getClaims(String token){
        try {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (SignatureException | ExpiredJwtException e) { // Invalid signature or expired token
            throw new AccessDeniedException("Access denied: " + e.getMessage());
        }

    }

    //The logic has issue that not able to check empty token
    public boolean isExpired(String token){
        try{
            return getClaims(token).getExpiration().before(new Date());
        }catch (Exception e){
            return false;
        }
    }


}
