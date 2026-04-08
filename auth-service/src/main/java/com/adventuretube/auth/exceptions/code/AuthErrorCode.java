package com.adventuretube.auth.exceptions.code;

import com.adventuretube.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {

    // --- 400 BAD_REQUEST ---
    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),

    // --- 401 UNAUTHORIZED ---
    USER_NOT_FOUND("User not found", HttpStatus.UNAUTHORIZED),
    USER_CREDENTIALS_INVALID("Invalid username or password", HttpStatus.UNAUTHORIZED),
    GOOGLE_TOKEN_INVALID("Google ID token is not valid", HttpStatus.UNAUTHORIZED),
    GOOGLE_TOKEN_MALFORMED("Google ID token is malformed", HttpStatus.UNAUTHORIZED),
    GOOGLE_EMAIL_MISMATCH("Email does not match between request and google_id token", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("Token not found", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token expired", HttpStatus.UNAUTHORIZED),
    TOKEN_DELETION_FAILED("Failed to delete token during logout", HttpStatus.UNAUTHORIZED),
    TOKEN_SAVE_FAILED("Failed to save token during login", HttpStatus.UNAUTHORIZED),
    USER_EMAIL_DUPLICATE("User already exists with the provided email", HttpStatus.UNAUTHORIZED),

    // --- 500 INTERNAL_SERVER_ERROR ---
    MEMBER_REGISTRATION_FAILED("Failed to register member", HttpStatus.INTERNAL_SERVER_ERROR),  // NOT USED — dead code, no reference
    MEMBER_DELETION_FAILED("Failed to delete user", HttpStatus.INTERNAL_SERVER_ERROR),  // NOT USED — deleteUser() endpoint not called by iOS
    INTERNAL_ERROR("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNKNOWN_EXCEPTION("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- 503 SERVICE_UNAVAILABLE ---
    SERVER_NOT_AVAILABLE("Server is not available", HttpStatus.SERVICE_UNAVAILABLE),
    SERVICE_CIRCUIT_OPEN("Member service is temporarily unavailable, please try again later", HttpStatus.SERVICE_UNAVAILABLE);

    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
