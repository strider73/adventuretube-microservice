package com.adventuretube.member.exceptions;


import com.adventuretube.common.api.code.ErrorCode;
import com.adventuretube.common.api.code.SystemErrorCode;
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

        ErrorCode code = ex.getErrorCode();
        ServiceResponse<Void> restAPIErrorResponse = ServiceResponse.<Void>builder()
                .success(false)
                .message(code.getMessage() + " : member-service")
                .errorCode(code.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return new ResponseEntity<>(restAPIErrorResponse, code.getHttpStatus());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse> handleUnknownException(Exception ex) {

        ServiceResponse restAPIErrorResponse = ServiceResponse.builder()
                .success(false)
                .message(SystemErrorCode.UNKNOWN_EXCEPTION.getMessage() + " : member-service")
                .errorCode(SystemErrorCode.UNKNOWN_EXCEPTION.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
