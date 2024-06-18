package com.adventuretube.apigateway.exception;


import com.adventuretube.common.error.RestAPIErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.SignatureException;
import java.time.Instant;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<RestAPIErrorResponse> handleSigniturexception(SignatureException ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "Invalid JWT signature",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<RestAPIErrorResponse> handleMalformedJwtxception(MalformedJwtException ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "MalformedJwt JWT token",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<RestAPIErrorResponse> handleExpiredJwtException(ExpiredJwtException ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "Expired JWT token",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIErrorResponse> handleUnknownException(Exception ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "Internal Server Error",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
