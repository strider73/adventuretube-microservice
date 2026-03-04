package com.adventuretube.web.exceptions.base;

import com.adventuretube.web.exceptions.code.WebErrorCode;

public abstract class BaseServiceException extends RuntimeException {
    private final WebErrorCode errorCode;
    private final String origin;

    public BaseServiceException(WebErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;

        // Automatically capture className.methodName
        StackTraceElement[] stackTrace = this.getStackTrace();
        this.origin = stackTrace.length > 0
                ? stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName()
                : "UnknownOrigin";
    }

    public WebErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOrigin() {
        return origin;
    }
}
