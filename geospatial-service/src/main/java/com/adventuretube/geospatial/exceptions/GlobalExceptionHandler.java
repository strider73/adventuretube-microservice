package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.api.response.ServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<?>> handleUnknownException(Exception ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message("Internal Server Error: geospatial-service")
                .errorCode(ex.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message("User does not exist: geospatial-service")
                .errorCode(ex.getMessage())
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
