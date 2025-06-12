package com.adventuretube.apigateway.exception;

import com.adventuretube.common.api.code.JwtErrorCode;
import com.adventuretube.common.api.code.SystemErrorCode;
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
                .status(JwtErrorCode.TOKEN_NOT_EXIST.getHttpStatus())
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message(JwtErrorCode.TOKEN_NOT_EXIST.getMessage() + ": gateway-service")
                        .errorCode(JwtErrorCode.TOKEN_NOT_EXIST.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ServiceResponse<Void>> handleSignatureException(SignatureException ex) {
        return ResponseEntity
                .status(JwtErrorCode.TOKEN_INVALID.getHttpStatus())
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message(JwtErrorCode.TOKEN_INVALID.getMessage() + ": gateway-service")
                        .errorCode(JwtErrorCode.TOKEN_INVALID.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ServiceResponse<Void>> handleMalformedJwtException(MalformedJwtException ex) {
        return ResponseEntity
                .status(JwtErrorCode.TOKEN_MALFORMED.getHttpStatus())
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message(JwtErrorCode.TOKEN_MALFORMED.getMessage() + ": gateway-service")
                        .errorCode(JwtErrorCode.TOKEN_MALFORMED.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ServiceResponse<Void>> handleExpiredJwtException(ExpiredJwtException ex) {
        return ResponseEntity
                .status(JwtErrorCode.TOKEN_EXPIRED.getHttpStatus())
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message(JwtErrorCode.TOKEN_EXPIRED.getMessage() + ": gateway-service")
                        .errorCode(JwtErrorCode.TOKEN_EXPIRED.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<Void>> handleUnknownException(Exception ex) {
        return ResponseEntity
                .status(SystemErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ServiceResponse.<Void>builder()
                        .success(false)
                        .message(SystemErrorCode.INTERNAL_ERROR.getMessage() + ": gateway-service")
                        .errorCode(SystemErrorCode.INTERNAL_ERROR.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }
}
