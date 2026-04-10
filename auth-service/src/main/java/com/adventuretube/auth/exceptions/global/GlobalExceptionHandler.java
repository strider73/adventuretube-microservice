package com.adventuretube.auth.exceptions.global;

import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClientException;
import com.adventuretube.common.exception.BaseServiceException;
  import com.adventuretube.common.exception.ErrorCode;import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    // WebExchangeBindException is only triggered in Controller layer with @Valid, @NotNull, @NotEmpty, @Size, @Email, @Pattern, etc.
    // It is triggered while serializing the request body to whatever parameter type in controller layer method.
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ServiceResponse<?>> handleValidationExceptions(WebExchangeBindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);

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
    // For exceptions that do NOT extend BaseServiceException (e.g. Spring's
    // UsernameNotFoundException, GeneralSecurityException, IllegalStateException).
    // These have no getOrigin(), so data is null.
    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(ErrorCode errorCode) {
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

    // For custom exceptions that extend BaseServiceException.
    // These carry getOrigin() — the class/method where the exception was created —
    // which is included in the data field for traceability.
    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(ErrorCode errorCode, BaseServiceException ex) {
        return new ResponseEntity<>(
                ServiceResponse.builder()
                        .success(false)
                        .message(errorCode.getMessage())
                        .errorCode(errorCode.name())
                        .data(ex.getOriginMethod() + " : auth-service")
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
        log.warn("Username not found: {}", ex.getMessage());
        return buildErrorResponse(AuthErrorCode.USER_NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ServiceResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(AuthErrorCode.USER_CREDENTIALS_INVALID);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ServiceResponse<?>> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException ex) {
        log.error("Internal authentication service error", ex);
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
        log.error("Google security exception", ex);
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
        if (ex.isServerError()) {
            log.error("Inter-service call failed [{}] {}", ex.getErrorCode(), ex.getMessage(), ex);
        } else {
            log.warn("Inter-service client error [{}] {}", ex.getErrorCode(), ex.getMessage());
        }
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
        log.error("Illegal state", ex);
        return buildErrorResponse(AuthErrorCode.SERVER_NOT_AVAILABLE);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ServiceResponse<?>> handleInternalServerException(InternalServerException ex) {
        log.error("Internal server error: {} [origin={}]", ex.getErrorCode().getMessage(), ex.getOriginMethod(), ex);
        return buildErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<?>> handleUnknownException(Exception ex) {
        log.error("Unhandled exception: {} - {}", ex.getClass().getName(), ex.getMessage(), ex);
        return buildErrorResponse(AuthErrorCode.INTERNAL_ERROR);
    }
}
