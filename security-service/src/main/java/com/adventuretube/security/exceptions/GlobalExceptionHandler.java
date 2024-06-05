package com.adventuretube.security.exceptions;


import com.adventuretube.common.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicationException(DuplicateException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                "User already exists with the provided email",
                HttpStatus.CONFLICT.value(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(errorResponse,HttpStatus.CONFLICT);
    }
}
