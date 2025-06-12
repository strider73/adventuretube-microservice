package com.adventuretube.apigateway.exception;

import com.adventuretube.common.api.response.ServiceResponse;
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
    public ResponseEntity<ServiceResponse<Void>> handleJwtTokenNotExistException(JwtTokenNotExistException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .errorCode("JWT_TOKEN_NOT_EXIST : gateway-service")
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ServiceResponse<Void>> handleSignatureException(SignatureException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message("Invalid JWT signature: gateway-service")
                        .errorCode(ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ServiceResponse<Void>> handleMalformedJwtException(MalformedJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message("Malformed JWT token: gateway-service")
                        .errorCode(ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ServiceResponse<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message("Expired JWT token: gateway-service")
                        .errorCode(ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<Void>> handleUnknownException(Exception ex) {
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message("Internal Server Error: gateway-service")
                        .errorCode(ex.getMessage())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }
}
