package com.adventuretube.auth.exceptions.code;

import com.adventuretube.common.api.code.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {
    //Google Exceptions
    GOOGLE_TOKEN_INVALID("Google ID token is not valid", HttpStatus.UNAUTHORIZED),
    GOOGLE_TOKEN_MALFORMED("Google ID token is malformed ", HttpStatus.UNAUTHORIZED),
    GOOGLE_EMAIL_MISMATCH("Email does not match between request and google_id token", HttpStatus.UNAUTHORIZED),

    //User Exceptions
    USER_EMAIL_DUPLICATE("User already exists with the provided email", HttpStatus.CONFLICT),
    USER_CREDENTIALS_INVALID("Invalid username or password", HttpStatus.UNAUTHORIZED),


    //System Exceptions
    USER_DOES_NOT_EXIST("User does not exist", HttpStatus.NOT_FOUND),

    //JWT Exceptions
    //Member Service Exceptions
    MEMBER_REGISTRATION_FAILED("Failed to register member", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    AuthErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
