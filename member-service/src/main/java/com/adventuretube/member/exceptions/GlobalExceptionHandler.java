package com.adventuretube.member.exceptions;


import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.exceptions.code.MemberErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ServiceResponse> buildErrorResponse(MemberErrorCode code, String origin) {
        ServiceResponse response = ServiceResponse.builder()
                .success(false)
                .message(code.getMessage())
                .errorCode(code.name())
                .data(origin)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return new ResponseEntity<>(response, code.getHttpStatus());
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ServiceResponse> handleDuplicationException(DuplicateException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getOrigin() + " : member-service");
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ServiceResponse> handleMemberNotFoundException(MemberNotFoundException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getOrigin() + " : member-service");
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceResponse> handleUnknownException(Exception ex) {
        return buildErrorResponse(MemberErrorCode.UNKNOWN_EXCEPTION, "member-service");
    }
}
