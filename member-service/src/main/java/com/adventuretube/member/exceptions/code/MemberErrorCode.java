package com.adventuretube.member.exceptions.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum MemberErrorCode {
    ENV_FILE_NOT_FOUND("ENV_FILE_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR),

    USER_EMAIL_DUPLICATE("User already exists with the provided email", HttpStatus.CONFLICT),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),

    //UNKNOWN EXCEPTION
    UNKNOWN_EXCEPTION("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);


    private final String message;
    private final HttpStatus httpStatus;

    MemberErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
