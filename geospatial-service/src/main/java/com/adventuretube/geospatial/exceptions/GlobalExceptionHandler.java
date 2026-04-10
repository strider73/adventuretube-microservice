package com.adventuretube.geospatial.exceptions;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleDataNotFoundException(DataNotFoundException ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(ex.getErrorCode().getMessage())
                .errorCode(ex.getErrorCode().name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(DuplicateDataException.class)
    public ResponseEntity<ServiceResponse<?>> handleDuplicateDataException(DuplicateDataException ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(ex.getErrorCode().getMessage())
                .errorCode(ex.getErrorCode().name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ServiceResponse<?>> handleDuplicateKeyException(DuplicateKeyException ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(GeoErrorCode.DUPLICATE_KEY.getMessage() + ": " + ex.getMostSpecificCause().getMessage())
                .errorCode(GeoErrorCode.DUPLICATE_KEY.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ServiceResponse<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument", ex);
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(GeoErrorCode.DATA_NOT_FOUND.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<?>> handleUnknownException(Exception ex) {
        log.error("Unhandled exception: {} - {}", ex.getClass().getName(), ex.getMessage(), ex);
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(GeoErrorCode.UNKNOWN_EXCEPTION.getMessage() + ": geospatial-service")
                .errorCode(GeoErrorCode.UNKNOWN_EXCEPTION.name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleJobNotFoundException(JobNotFoundException ex) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(ex.getErrorCode().getMessage())
                .errorCode(ex.getErrorCode().name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(OwnershipMismatchException.class)
    public ResponseEntity<ServiceResponse<?>> handleOwnershipMismatchException(OwnershipMismatchException ex) {
        ServiceResponse<?>  response = ServiceResponse.builder()
                .success(false)
                .message(ex.getErrorCode().getMessage())
                .errorCode(ex.getErrorCode().name())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return  ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);

    }
}
