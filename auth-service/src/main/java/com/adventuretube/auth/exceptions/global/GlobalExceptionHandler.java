package com.adventuretube.auth.exceptions.global;

import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.exceptions.base.BaseServiceException;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.auth.common.response.RestAPIResponse;
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
    public ResponseEntity<RestAPIResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        RestAPIResponse response = RestAPIResponse.builder()
                .message(AuthErrorCode.VALIDATION_FAILED.getMessage())
                .details(errors.toString()+" : auth-service")
                .statusCode(AuthErrorCode.VALIDATION_FAILED.getHttpStatus().value())
                .timestamp(System.currentTimeMillis())
                .build();

        return new ResponseEntity<>(response, AuthErrorCode.VALIDATION_FAILED.getHttpStatus());
    }




    private ResponseEntity<RestAPIResponse> buildErrorResponse(AuthErrorCode errorCode) {
        RestAPIResponse response = RestAPIResponse.builder()
                .message(errorCode.getMessage())
                .details("Generated by: " +" : auth-service") // or some static message
                .statusCode(errorCode.getHttpStatus().value())
                .timestamp(System.currentTimeMillis())
                .build();
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    private ResponseEntity<RestAPIResponse> buildErrorResponse(AuthErrorCode errorCode, BaseServiceException ex) {
        RestAPIResponse response = RestAPIResponse.builder()
                .message(errorCode.getMessage())
                .details(ex.getOrigin() + " : auth-service")
                .statusCode(errorCode.getHttpStatus().value())
                .timestamp(System.currentTimeMillis())
                .build();
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }



    // ---------------------------
    // Authentication Exceptions
    // ---------------------------

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

    // ---------------------------
    // Google Token Exceptions
    // ---------------------------

    @ExceptionHandler(GoogleIdTokenInvalidException.class)
    public ResponseEntity<RestAPIResponse> handleGoogleIdTokenInvalidException(GoogleIdTokenInvalidException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<RestAPIResponse> handleGeneralSecurityException(GeneralSecurityException ex) {
        return buildErrorResponse(AuthErrorCode.GOOGLE_TOKEN_MALFORMED);
    }

    // ---------------------------
    // Token Management Exceptions
    // ---------------------------

    @ExceptionHandler(TokenSaveFailedException.class)
    public ResponseEntity<RestAPIResponse> handleTokenStoreException(TokenSaveFailedException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }

    @ExceptionHandler(TokenDeletionException.class)
    public ResponseEntity<RestAPIResponse> handleTokenDeletionException(TokenDeletionException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<RestAPIResponse> handleTokenNotFoundException(TokenNotFoundException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<RestAPIResponse> handleTokenExpiredException(TokenExpiredException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }
    // ---------------------------
    // Member Service Exceptions
    // ---------------------------

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<RestAPIResponse> handleDuplicationException(DuplicateException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }

    @ExceptionHandler(MemberServiceException.class)
    public ResponseEntity<RestAPIResponse> handleMemberServiceException(MemberServiceException ex) {
        return buildErrorResponse(ex.getErrorCode(),ex);
    }

    // ---------------------------
    // Server & Unknown Exceptions
    // ---------------------------

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RestAPIResponse> handleIllegalStateException(IllegalStateException ex) {
        return buildErrorResponse(AuthErrorCode.SERVER_NOT_AVAILABLE);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<RestAPIResponse> handleInternalServerException(InternalServerException ex) {
        return buildErrorResponse(ex.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIResponse> handleUnknownException(Exception ex) {
        return buildErrorResponse(AuthErrorCode.INTERNAL_ERROR);
    }
}
