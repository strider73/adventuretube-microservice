package com.adventuretube.auth.exceptions.base;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;

public abstract class BaseServiceException extends RuntimeException {
    private final AuthErrorCode errorCode;
    private final String origin;

    public BaseServiceException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;

        // Automatically capture className.methodName
        StackTraceElement[] stackTrace = this.getStackTrace();
        this.origin = stackTrace.length > 0
                ? stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName()
                : "UnknownOrigin";
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOrigin() {
        return origin;
    }
}
