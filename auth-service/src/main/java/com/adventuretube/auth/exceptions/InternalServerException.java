package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class InternalServerException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public InternalServerException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
