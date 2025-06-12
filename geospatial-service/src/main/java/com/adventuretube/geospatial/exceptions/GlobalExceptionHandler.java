package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.api.code.SystemErrorCode;
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
                .message(SystemErrorCode.UNKNOWN_EXCEPTION.getMessage() + ": geospatial-service")
                .errorCode(SystemErrorCode.UNKNOWN_EXCEPTION.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(SystemErrorCode.USER_NOT_FOUND.getMessage() + ": geospatial-service")
                .errorCode(SystemErrorCode.USER_NOT_FOUND.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
