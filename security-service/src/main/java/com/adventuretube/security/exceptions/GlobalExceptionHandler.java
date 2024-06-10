package com.adventuretube.security.exceptions;


import com.adventuretube.common.error.RestAPIErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<RestAPIErrorResponse> handleGeneralSecurityException(GeneralSecurityException ex){
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "Google Id token is not able to verify",
                HttpStatus.UNAUTHORIZED.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse,HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(GoogleIdTokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<RestAPIErrorResponse> handleBadRequestExceptions(GoogleIdTokenInvalidException ex){
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
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
    public ResponseEntity<RestAPIErrorResponse> handleDuplicationException(DuplicateException ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "User already exists with the provided email",
                HttpStatus.CONFLICT.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse,HttpStatus.CONFLICT);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIErrorResponse> handleUnknownException(Exception ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "Unknown Error",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
