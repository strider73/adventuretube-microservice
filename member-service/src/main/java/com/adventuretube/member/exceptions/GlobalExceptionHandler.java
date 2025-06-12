package com.adventuretube.member.exceptions;


import com.adventuretube.common.api.response.ServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ServiceResponse<Void>> handleDuplicationException(DuplicateException ex) {

        ServiceResponse restAPIErrorResponse = ServiceResponse.builder()
                .success(false)
                .message("User already exists with the provided email : member-service")
                .errorCode("DUPLICATE_ERROR")
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return new ResponseEntity<ServiceResponse<Void>>(restAPIErrorResponse,HttpStatus.CONFLICT);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse> handleUnknownException(Exception ex) {

        ServiceResponse restAPIErrorResponse = ServiceResponse.builder()
                .success(false)
                .message("Internal Server Error : member-service")
                .errorCode("UNKNOWN_ERROR")
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

            return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
