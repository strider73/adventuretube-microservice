package com.adventuretube.auth.exceptions;

public class UserNotFoundException extends RuntimeException{
    private final AuthErrorCode errorCode;

    public UserNotFoundException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
