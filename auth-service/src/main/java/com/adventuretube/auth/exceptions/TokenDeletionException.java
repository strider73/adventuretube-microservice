package com.adventuretube.auth.exceptions;

public class TokenDeletionException extends RuntimeException {
    private final AuthErrorCode errorCode;

    public TokenDeletionException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
