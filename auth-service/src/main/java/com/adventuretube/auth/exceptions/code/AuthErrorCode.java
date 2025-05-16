package com.adventuretube.auth.exceptions.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode {
    //Google Exceptions
    GOOGLE_TOKEN_INVALID("Google ID token is not valid", HttpStatus.UNAUTHORIZED),
    GOOGLE_TOKEN_MALFORMED("Google ID token is malformed ", HttpStatus.UNAUTHORIZED),
    GOOGLE_EMAIL_MISMATCH("Email does not match between request and google_id token", HttpStatus.UNAUTHORIZED),

    //User Exceptions
    USER_EMAIL_DUPLICATE("User already exists with the provided email", HttpStatus.CONFLICT),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    USER_CREDENTIALS_INVALID("Invalid username or password", HttpStatus.UNAUTHORIZED),


    //System Exceptions
    SERVER_NOT_AVAILABLE("Server is not available", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED("Validation failed", HttpStatus.BAD_REQUEST),
    USER_DOES_NOT_EXIST("User does not exist", HttpStatus.NOT_FOUND),

    //JWT Exceptions
    TOKEN_DELETION_FAILED("Failed to delete token during logout", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_SAVE_FAILED("Failed to save token during login", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_NOT_FOUND("Token not found", HttpStatus.NOT_FOUND),
    TOKEN_EXPIRED("Token expired", HttpStatus.UNAUTHORIZED),
    //Member Service Exceptions
    MEMBER_REGISTRATION_FAILED("Failed to register member", HttpStatus.INTERNAL_SERVER_ERROR),
    
    
    //UNKNOWN EXCEPTION
    UNKNOWN_EXCEPTION("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
