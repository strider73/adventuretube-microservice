package com.adventuretube.auth.exceptions;


import com.adventuretube.common.error.RestAPIResponse;
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

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<RestAPIResponse> buildErrorResponse(AuthErrorCode errorCode) {
        RestAPIResponse response = RestAPIResponse.builder()
                .message(errorCode.getMessage())
                .statusCode(errorCode.getHttpStatus().value())
                .timestamp(System.currentTimeMillis())
                .build();
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<RestAPIResponse> handleDuplicationException(DuplicateException ex) {
        return buildErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(GoogleIdTokenInvalidException.class)
    public ResponseEntity<RestAPIResponse> handleGoogleIdTokenInvalidException(GoogleIdTokenInvalidException ex) {
        return buildErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<RestAPIResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return buildErrorResponse(AuthErrorCode.USER_NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RestAPIResponse> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(AuthErrorCode.USER_CREDENTIALS_INVALID);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<RestAPIResponse> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException ex) {
        return buildErrorResponse(AuthErrorCode.USER_DOES_NOT_EXIST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RestAPIResponse> handleIllegalStateException(IllegalStateException ex) {
        return buildErrorResponse(AuthErrorCode.SERVER_NOT_AVAILABLE);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<RestAPIResponse> handleGeneralSecurityException(GeneralSecurityException ex) {
        return buildErrorResponse(AuthErrorCode.GOOGLE_TOKEN_MALFORMED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<RestAPIResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        RestAPIResponse response = RestAPIResponse.builder()
                .message(AuthErrorCode.VALIDATION_FAILED.getMessage())
                .details(errors.toString())
                .statusCode(AuthErrorCode.VALIDATION_FAILED.getHttpStatus().value())
                .timestamp(System.currentTimeMillis())
                .build();

        return new ResponseEntity<>(response, AuthErrorCode.VALIDATION_FAILED.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIResponse> handleUnknownException(Exception ex) {
        return buildErrorResponse(AuthErrorCode.INTERNAL_ERROR);
    }
}
