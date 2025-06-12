package com.adventuretube.common.api.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SystemErrorCode implements ErrorCode {
    SERVER_NOT_AVAILABLE("Server is not available", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    UNKNOWN_EXCEPTION("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;
}
