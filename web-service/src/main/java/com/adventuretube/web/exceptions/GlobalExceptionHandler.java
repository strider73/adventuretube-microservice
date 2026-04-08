package com.adventuretube.web.exceptions;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClientException;
import com.adventuretube.common.exception.BaseServiceException;
import com.adventuretube.common.exception.ErrorCode;
import com.adventuretube.web.exceptions.code.WebErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GeoServiceException.class)
    public ResponseEntity<ServiceResponse<?>> handleGeoServiceException(GeoServiceException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex);
    }

    @ExceptionHandler(ServiceClientException.class)
    public ResponseEntity<ServiceResponse<?>> handleServiceClientException(ServiceClientException ex) {
        log.error("Inter-service call failed: {}", ex.toString());
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .data(ex.getServiceName() + " : web-service")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<?>> handleUnknownException(Exception ex) {
        log.error("Unhandled exception: {} - {}", ex.getClass().getName(), ex.getMessage(), ex);
        return buildErrorResponse(WebErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(ErrorCode errorCode) {
        return new ResponseEntity<>(
                ServiceResponse.builder()
                        .success(false)
                        .message(errorCode.getMessage())
                        .errorCode(errorCode.name())
                        .data(null)
                        .timestamp(java.time.LocalDateTime.now())
                        .build(),
                errorCode.getHttpStatus()
        );
    }

    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(ErrorCode errorCode, BaseServiceException ex) {
        return new ResponseEntity<>(
                ServiceResponse.builder()
                        .success(false)
                        .message(errorCode.getMessage())
                        .errorCode(errorCode.name())
                        .data(ex.getOrigin() + " : web-service")
                        .timestamp(java.time.LocalDateTime.now())
                        .build(),
                errorCode.getHttpStatus()
        );
    }
}
