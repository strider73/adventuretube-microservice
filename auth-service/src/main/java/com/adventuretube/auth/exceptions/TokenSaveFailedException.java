package com.adventuretube.auth.exceptions;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public class TokenSaveFailedException extends RuntimeException {
    private final AuthErrorCode errorCode;
    public TokenSaveFailedException(AuthErrorCode authErrorCode) {
        super(authErrorCode.getMessage());
        this.errorCode = authErrorCode;
    }
    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
