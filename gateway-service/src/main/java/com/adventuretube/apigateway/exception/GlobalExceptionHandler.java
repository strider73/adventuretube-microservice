package com.adventuretube.apigateway.exception;


import com.adventuretube.common.error.RestAPIResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.SignatureException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtTokenNotExistException.class)
    public ResponseEntity<RestAPIResponse> handleJwtTokenNotExistexception(JwtTokenNotExistException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "JWT token not exist : gateway -service ",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<RestAPIResponse> handleSigniturexception(SignatureException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Invalid JWT signature: gateway -service",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<RestAPIResponse> handleMalformedJwtxception(MalformedJwtException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "MalformedJwt JWT token: gateway -service",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<RestAPIResponse> handleExpiredJwtException(ExpiredJwtException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Expired JWT token: gateway -service",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIResponse> handleUnknownException(Exception ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Internal Server Error: gateway -service",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
