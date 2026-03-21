package com.adventuretube.auth.exceptions.global;

import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------
    // Validation & Input Handling
    // ---------------------------
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ServiceResponse<?>> handleValidationExceptions(WebExchangeBindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(AuthErrorCode.VALIDATION_FAILED.getMessage())
                .errorCode(AuthErrorCode.VALIDATION_FAILED.name())
                .data(errors)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, AuthErrorCode.VALIDATION_FAILED.getHttpStatus());
    }

    // ---------------------------
    // Helper Methods
    // ---------------------------
    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(AuthErrorCode errorCode) {
        return new ResponseEntity<>(
                ServiceResponse.builder()
                        .success(false)
                        .message(errorCode.getMessage())
                        .errorCode(errorCode.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build(),
                errorCode.getHttpStatus()
        );
    }

    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(AuthErrorCode errorCode, BaseServiceException ex) {
        return new ResponseEntity<>(
                ServiceResponse.builder()
                        .success(false)
                        .message(errorCode.getMessage())
                        .errorCode(errorCode.name())
                        .data(ex.getOrigin() + " : auth-service")
                        .timestamp(java.time.LocalDateTime.now())
                        .build(),
                errorCode.getHttpStatus()
        );
    }

    // ---------------------------
    // Authentication Exceptions
    // ---------------------------
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return buildErrorResponse(AuthErrorCode.USER_NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ServiceResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(AuthErrorCode.USER_CREDENTIALS_INVALID);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ServiceResponse<?>> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException ex) {
        return buildErrorResponse(AuthErrorCode.USER_NOT_FOUND);
    }

    // ---------------------------
    // Google Token Exceptions
    // ---------------------------
    @ExceptionHandler(GoogleIdTokenInvalidException.class)
    public ResponseEntity<ServiceResponse<?>> handleGoogleIdTokenInvalidException(GoogleIdTokenInvalidException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<ServiceResponse<?>> handleGeneralSecurityException(GeneralSecurityException ex) {
        return buildErrorResponse(AuthErrorCode.GOOGLE_TOKEN_MALFORMED);
    }

    // ---------------------------
    // Token Management Exceptions
    // ---------------------------
    @ExceptionHandler(TokenSaveFailedException.class)
    public ResponseEntity<ServiceResponse<?>> handleTokenStoreException(TokenSaveFailedException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(TokenDeletionException.class)
    public ResponseEntity<ServiceResponse<?>> handleTokenDeletionException(TokenDeletionException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleTokenNotFoundException(TokenNotFoundException ex) {
        log.warn("Token not found: {} [origin={}]", ex.getErrorCode().getMessage(), ex.getOrigin());
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ServiceResponse<?>> handleTokenExpiredException(TokenExpiredException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ServiceResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    // ---------------------------
    // Member Service Exceptions
    // ---------------------------
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleUserNotFoundException(UserNotFoundException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ServiceResponse<?>> handleDuplicationException(DuplicateException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(MemberServiceException.class)
    public ResponseEntity<ServiceResponse<?>> handleMemberServiceException(MemberServiceException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    // ---------------------------
    // Inter-Service Communication
    // ---------------------------
    @ExceptionHandler(ServiceClientException.class)
    public ResponseEntity<ServiceResponse<?>> handleServiceClientException(ServiceClientException ex) {
        log.error("Inter-service call failed: {}", ex.toString());
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .data(ex.getServiceName() + " : auth-service")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // ---------------------------
    // Server & Unknown Exceptions
    // ---------------------------
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ServiceResponse<?>> handleIllegalStateException(IllegalStateException ex) {
        return buildErrorResponse(AuthErrorCode.SERVER_NOT_AVAILABLE);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ServiceResponse<?>> handleInternalServerException(InternalServerException ex) {
        return buildErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<?>> handleUnknownException(Exception ex) {
        log.error("Unhandled exception: {} - {}", ex.getClass().getName(), ex.getMessage(), ex);
        return buildErrorResponse(AuthErrorCode.INTERNAL_ERROR);
    }
}
