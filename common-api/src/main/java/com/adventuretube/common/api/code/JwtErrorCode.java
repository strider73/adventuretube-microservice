package com.adventuretube.common.api.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum JwtErrorCode implements ErrorCode {
    TOKEN_DELETION_FAILED("Failed to delete token during logout", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_SAVE_FAILED("Failed to save token during login", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_NOT_FOUND("Token not found", HttpStatus.NOT_FOUND),
    TOKEN_NOT_EXIST("Token is not exist", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("Invalid JWT signature", HttpStatus.UNAUTHORIZED),
    TOKEN_MALFORMED("Malformed JWT token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token expired", HttpStatus.UNAUTHORIZED);

    private final String message;
    private final HttpStatus httpStatus;
}
