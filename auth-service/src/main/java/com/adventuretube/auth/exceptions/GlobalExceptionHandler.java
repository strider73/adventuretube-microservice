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


    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<RestAPIResponse> handleGeneralSecurityException(GeneralSecurityException ex){
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Google Id token is not able to verify",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse,HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(GoogleIdTokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<RestAPIResponse> handleBadRequestExceptions(GoogleIdTokenInvalidException ex){
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Google idToken is not valid",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return  new ResponseEntity<>(restAPIErrorResponse,HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<RestAPIResponse> handleDuplicationException(DuplicateException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "User already exists with the provided email",
                HttpStatus.CONFLICT.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse,HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<RestAPIResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "User does mot exist",
                HttpStatus.NOT_FOUND.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RestAPIResponse> handleBadCredentialsException(BadCredentialsException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RestAPIResponse> handleRestClientException(IllegalStateException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Server is not available",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<RestAPIResponse> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "User is not exist",
                HttpStatus.NOT_FOUND.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse,  HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIResponse> handleUnknownException(Exception ex) {
        RestAPIResponse restAPIErrorResponse = new RestAPIResponse(
                ex.getMessage(),
                "Unknown Error",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }


}
