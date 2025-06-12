package com.adventuretube.auth.exceptions.global;

import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.common.api.response.ServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------
    // Validation & Input Handling
    // ---------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ServiceResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
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
        return buildErrorResponse(AuthErrorCode.USER_DOES_NOT_EXIST);
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
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ServiceResponse<?>> handleTokenExpiredException(TokenExpiredException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    // ---------------------------
    // Member Service Exceptions
    // ---------------------------
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ServiceResponse<?>> handleDuplicationException(DuplicateException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(MemberServiceException.class)
    public ResponseEntity<ServiceResponse<?>> handleMemberServiceException(MemberServiceException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
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
        return buildErrorResponse(AuthErrorCode.INTERNAL_ERROR);
    }
}
