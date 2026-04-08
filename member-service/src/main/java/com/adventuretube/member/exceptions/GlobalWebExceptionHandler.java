package com.adventuretube.member.exceptions;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.exception.ErrorCode;
import com.adventuretube.member.exceptions.code.MemberErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalWebExceptionHandler {

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ServiceResponse<?>> handleDuplicateException(DuplicateException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getOrigin() + " : member-service");
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ServiceResponse<?>> handleMemberNotFoundException(MemberNotFoundException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getOrigin() + " : member-service");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse<?>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(MemberErrorCode.UNKNOWN_EXCEPTION, "member-service");
    }

    private ResponseEntity<ServiceResponse<?>> buildErrorResponse(ErrorCode code, String origin) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(code.getMessage())
                .errorCode(code.name())
                .data(origin)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }
}
