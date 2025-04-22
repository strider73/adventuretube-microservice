package com.adventuretube.auth.exceptions;

public class DuplicateException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public DuplicateException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
